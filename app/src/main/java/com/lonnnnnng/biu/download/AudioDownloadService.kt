package com.lonnnnnng.biu.download

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.lonnnnnng.biu.MainActivity
import com.lonnnnnng.biu.appContainer
import com.lonnnnnng.biu.data.bilibili.DashAudioStream
import com.lonnnnnng.biu.data.local.AudioDownloadTaskEntity
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AudioDownloadService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val commandMutex = Mutex()
    private val repository by lazy { appContainer.audioDownloadRepository }
    private val bilibiliRepository by lazy { appContainer.bilibiliRepository }
    private val downloader by lazy { ResumableFileDownloader(appContainer.bilibiliHttpClient) }
    private val publisher by lazy { AudioDownloadPublisher(this) }
    private val networkPreferenceRepository by lazy { appContainer.downloadNetworkPreferenceRepository }
    private val networkMonitor by lazy { DownloadNetworkMonitor(this, ::onNetworkChanged) }

    @Volatile
    private var activeTaskId: String? = null

    @Volatile
    private var requestedStop: StopReason? = null

    @Volatile
    private var networkState: DownloadNetworkState = DownloadNetworkState.DISCONNECTED

    @Volatile
    private var waitingForNetwork = false

    private var activeJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        val taskId = intent.getStringExtra(EXTRA_TASK_ID).orEmpty()
        val request = if (action == ACTION_START) requestFromIntent(intent) else null
        if (activeTaskId == null) {
            val bootstrapTitle = request?.title ?: "音频下载"
            startAsForeground(
                notificationId(taskId.ifBlank { bootstrapTitle }),
                buildNotification(
                    taskId = taskId,
                    title = bootstrapTitle,
                    text = "正在准备下载",
                    status = AudioDownloadStatus.RESOLVING,
                    downloadedBytes = 0L,
                    totalBytes = 0L,
                ),
            )
        }
        networkState = networkMonitor.state()
        serviceScope.launch {
            // long: 冷启动先完成上次进程遗留任务的降级，再处理用户的新命令，避免恢复写入覆盖刚开始的下载状态。
            appContainer.audioDownloadRecovery.await()
            commandMutex.withLock {
                runCatching {
                    when (action) {
                        ACTION_START -> request?.let { enqueue(it) }
                        ACTION_RESUME -> resume(taskId)
                        ACTION_PAUSE -> pause(taskId)
                        // long: launch 代码块自带 CoroutineScope.cancel 扩展；使用独立业务名称，确保取消的是下载任务而不是命令协程。
                        ACTION_CANCEL -> cancelTask(taskId)
                        ACTION_DRAIN,
                        ACTION_REFRESH_NETWORK,
                        -> handleNetworkConstraintChanged()
                    }
                }.onFailure { error ->
                    Log.w(LOG_TAG, "Download command failed: ${error.javaClass.simpleName}")
                    stopForegroundAndSelfIfIdle()
                }
                if (activeJob?.isActive == true || waitingForNetwork) networkMonitor.start()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        downloader.cancelActiveCall()
        networkMonitor.close()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        val taskId = activeTaskId
        requestedStop = StopReason.PAUSE
        downloader.cancelActiveCall()
        activeJob?.cancel()
        if (!taskId.isNullOrBlank()) {
            // long: Android 15+ dataSync 配额到期后必须在系统截止时间内停止服务；先同步落盘暂停态，避免进程被杀后任务仍显示运行中。
            runCatching {
                runBlocking(Dispatchers.IO) {
                    transitionIfAllowed(taskId, AudioDownloadStatus.PAUSED)
                }
            }
        }
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf(startId)
    }

    private suspend fun enqueue(request: AudioDownloadRequest) {
        val tempFile = tempFile(request.taskId)
        val task = repository.enqueue(request, tempFile.absolutePath)
        when (task.downloadStatus) {
            AudioDownloadStatus.COMPLETED -> {
                postTaskNotification(task, "已保存到 Music/Biu", active = false)
                stopForegroundAndSelfIfIdle()
            }
            AudioDownloadStatus.QUEUED -> {
                if (activeJob?.isActive == true) {
                    postTaskNotification(task, "已加入下载队列", active = false)
                } else {
                    launchNextQueued()
                }
            }
            else -> Unit
        }
    }

    private suspend fun resume(taskId: String) {
        val existing = repository.find(taskId) ?: return stopForegroundAndSelfIfIdle()
        val task = repository.enqueue(existing.toRequest(), existing.tempFilePath)
        if (task.downloadStatus == AudioDownloadStatus.COMPLETED) {
            postTaskNotification(task, "已保存到 Music/Biu", active = false)
            return stopForegroundAndSelfIfIdle()
        }
        if (activeJob?.isActive == true) {
            postTaskNotification(task, "已加入下载队列", active = false)
        } else {
            launchNextQueued()
        }
    }

    private suspend fun pause(taskId: String) {
        if (taskId == activeTaskId) {
            requestedStop = StopReason.PAUSE
            downloader.cancelActiveCall()
            activeJob?.cancel()
            return
        }
        val task = repository.find(taskId) ?: return stopForegroundAndSelfIfIdle()
        val paused = transitionIfAllowed(task.taskId, AudioDownloadStatus.PAUSED)
            ?: return stopForegroundAndSelfIfIdle()
        postTaskNotification(paused, "下载已暂停", active = false)
        launchNextQueued()
    }

    private suspend fun cancelTask(taskId: String) {
        if (taskId == activeTaskId) {
            requestedStop = StopReason.CANCEL
            downloader.cancelActiveCall()
            activeJob?.cancel()
            return
        }
        val task = repository.find(taskId) ?: return stopForegroundAndSelfIfIdle()
        val cancelled = transitionIfAllowed(task.taskId, AudioDownloadStatus.CANCELLED)
            ?: return stopForegroundAndSelfIfIdle()
        clearCancelledTask(cancelled)
        launchNextQueued()
    }

    private suspend fun launchNextQueued() {
        if (activeJob?.isActive == true) return
        val next = repository.nextQueued() ?: run {
            waitingForNetwork = false
            return stopForegroundAndSelfIfIdle()
        }
        val preference = networkPreferenceRepository.current()
        if (!DownloadNetworkPolicy.isAllowed(preference, networkState)) {
            waitingForNetwork = true
            postTaskNotification(next, DownloadNetworkPolicy.waitingLabel(preference), active = true)
            return
        }
        waitingForNetwork = false
        activeTaskId = next.taskId
        requestedStop = null
        activeJob = serviceScope.launch { runTask(next.taskId) }
    }

    private suspend fun runTask(taskId: String) {
        try {
            var task = repository.transition(taskId, AudioDownloadStatus.RESOLVING) ?: return
            postTaskNotification(task, "正在解析标准 AAC 音频", active = true)
            val stream = bilibiliRepository.resolveStandardAudioStream(task.bvid, task.cid)
            if (!stream.codecs.contains("mp4a", ignoreCase = true)) {
                throw IOException("暂不支持该音频编码")
            }
            task = repository.transition(
                taskId = taskId,
                target = AudioDownloadStatus.DOWNLOADING,
                qualityLabel = stream.qualityLabel,
            ) ?: return
            postTaskNotification(task, "正在下载 · ${stream.qualityLabel}", active = true)
            val file = File(task.tempFilePath)
            val downloadedBytes = if (
                task.totalBytes > 0L && file.isFile && file.length() == task.totalBytes
            ) {
                task.totalBytes
            } else {
                download(task, stream, file)
            }
            repository.updateProgress(taskId, downloadedBytes, downloadedBytes)
            task = repository.transition(taskId, AudioDownloadStatus.PUBLISHING) ?: return
            postTaskNotification(task, "正在保存到 Music/Biu", active = true)
            val publishedUri = publisher.publish(task.toRequest(), file)
            repository.markCompleted(taskId, publishedUri.toString(), downloadedBytes)
            file.delete()
            val finished = repository.find(taskId)
            DownloadCompletionNotificationCoordinator(
                postCompletedNotification = finished?.let { completedTask ->
                    {
                        postTaskNotification(
                            completedTask,
                            "下载完成 · ${completedTask.qualityLabel}",
                            active = false,
                        )
                    }
                },
                removeForegroundNotification = { stopForeground(STOP_FOREGROUND_REMOVE) },
            ).complete()
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { applyRequestedStop(taskId) }
        } catch (error: Throwable) {
            withContext(NonCancellable) {
                if (requestedStop != null) {
                    applyRequestedStop(taskId)
                } else {
                    Log.w(LOG_TAG, "Download task failed: ${error.javaClass.simpleName}")
                    val failed = transitionIfAllowed(
                        taskId = taskId,
                        target = AudioDownloadStatus.FAILED,
                        errorMessage = safeErrorMessage(error),
                    )
                    failed?.let { postTaskNotification(it, it.errorMessage ?: "下载失败，请重试", active = false) }
                }
            }
        } finally {
            withContext(NonCancellable) {
                activeTaskId = null
                requestedStop = null
                activeJob = null
                commandMutex.withLock { launchNextQueued() }
            }
        }
    }

    private suspend fun download(
        task: AudioDownloadTaskEntity,
        stream: DashAudioStream,
        targetFile: File,
    ): Long {
        val existingBytes = targetFile.takeIf(File::isFile)?.length() ?: 0L
        if (existingBytes > 0L && task.totalBytes > 0L && existingBytes == task.totalBytes) {
            return existingBytes
        }
        var lastPersistedBytes = existingBytes
        var lastPersistedAtNanos = System.nanoTime()
        var resolvedTotalBytes = task.totalBytes
        val downloadedBytes = downloader.download(
            urls = listOf(stream.url) + stream.backupUrls,
            targetFile = targetFile,
        ) { bytes, totalBytes ->
            resolvedTotalBytes = totalBytes
            val nowNanos = System.nanoTime()
            if (
                bytes == totalBytes ||
                bytes < lastPersistedBytes ||
                bytes - lastPersistedBytes >= PROGRESS_PERSIST_BYTES ||
                nowNanos - lastPersistedAtNanos >= PROGRESS_PERSIST_NANOS
            ) {
                repository.updateProgress(task.taskId, bytes, totalBytes)
                postProgressNotification(task, bytes, totalBytes)
                lastPersistedBytes = bytes
                lastPersistedAtNanos = nowNanos
            }
        }
        val totalBytes = resolvedTotalBytes.takeIf { it > 0L } ?: downloadedBytes
        repository.updateProgress(task.taskId, downloadedBytes, totalBytes)
        postProgressNotification(task, downloadedBytes, totalBytes)
        return downloadedBytes
    }

    private suspend fun applyRequestedStop(taskId: String) {
        when (requestedStop) {
            StopReason.PAUSE -> {
                val paused = transitionIfAllowed(taskId, AudioDownloadStatus.PAUSED)
                paused?.let { postTaskNotification(it, "下载已暂停", active = false) }
            }
            StopReason.CANCEL -> {
                val cancelled = transitionIfAllowed(taskId, AudioDownloadStatus.CANCELLED)
                cancelled?.let { task -> clearCancelledTask(task) }
            }
            StopReason.NETWORK -> {
                val paused = transitionIfAllowed(taskId, AudioDownloadStatus.PAUSED)
                paused?.let { task ->
                    val queued = repository.enqueue(task.toRequest(), task.tempFilePath)
                    postTaskNotification(
                        queued,
                        DownloadNetworkPolicy.waitingLabel(networkPreferenceRepository.current()),
                        active = false,
                    )
                }
            }
            null -> Unit
        }
        stopForeground(STOP_FOREGROUND_DETACH)
    }

    private fun onNetworkChanged(state: DownloadNetworkState) {
        networkState = state
        serviceScope.launch {
            appContainer.audioDownloadRecovery.await()
            commandMutex.withLock { handleNetworkConstraintChanged() }
        }
    }

    private suspend fun handleNetworkConstraintChanged() {
        if (!isNetworkAllowed()) {
            val taskId = activeTaskId
            if (!taskId.isNullOrBlank() && requestedStop == null) {
                // long: 用户选择仅非计费网络后，Wi-Fi 切到移动数据必须立即停止网络流并保留断点，避免后台继续消耗流量。
                requestedStop = StopReason.NETWORK
                downloader.cancelActiveCall()
                activeJob?.cancel()
            } else if (taskId.isNullOrBlank()) {
                launchNextQueued()
            }
            return
        }

        waitingForNetwork = false
        launchNextQueued()
    }

    private suspend fun isNetworkAllowed(): Boolean {
        return DownloadNetworkPolicy.isAllowed(networkPreferenceRepository.current(), networkState)
    }

    private suspend fun clearCancelledTask(task: AudioDownloadTaskEntity) {
        File(task.tempFilePath).delete()
        // long: 取消会删除断点文件，Room 同步归零进度，避免任务面板继续显示一段实际上已经不存在的已下载数据。
        repository.updateProgress(task.taskId, downloadedBytes = 0L, totalBytes = 0L)
        val cleared = repository.find(task.taskId) ?: task.copy(downloadedBytes = 0L, totalBytes = 0L)
        postTaskNotification(cleared, "下载已取消", active = false)
    }

    private suspend fun transitionIfAllowed(
        taskId: String,
        target: AudioDownloadStatus,
        errorMessage: String? = null,
    ): AudioDownloadTaskEntity? {
        val current = repository.find(taskId) ?: return null
        if (current.downloadStatus == target) return current
        if (!AudioDownloadStatePolicy.canTransition(current.downloadStatus, target)) {
            // long: 已完成任务可能收到旧通知按钮触发的迟到命令；拒绝转换时不能继续执行清文件、清进度或发送错误状态通知。
            return null
        }
        return repository.transition(taskId, target, errorMessage = errorMessage)
    }

    private fun postProgressNotification(task: AudioDownloadTaskEntity, downloadedBytes: Long, totalBytes: Long) {
        val text = if (totalBytes > 0L) {
            "正在下载 · ${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}"
        } else {
            "正在下载 · ${formatBytes(downloadedBytes)}"
        }
        val notification = buildNotification(
            taskId = task.taskId,
            title = task.title,
            text = text,
            status = AudioDownloadStatus.DOWNLOADING,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
        )
        startAsForeground(notificationId(task.taskId), notification)
    }

    private fun postTaskNotification(task: AudioDownloadTaskEntity, text: String, active: Boolean) {
        val notification = buildNotification(
            taskId = task.taskId,
            title = task.title,
            text = text,
            status = task.downloadStatus,
            downloadedBytes = task.downloadedBytes,
            totalBytes = task.totalBytes,
        )
        if (active) {
            startAsForeground(notificationId(task.taskId), notification)
        } else {
            notifySafely(notificationId(task.taskId), notification)
        }
    }

    private fun buildNotification(
        taskId: String,
        title: String,
        text: String,
        status: AudioDownloadStatus,
        downloadedBytes: Long,
        totalBytes: Long,
    ): Notification {
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppPendingIntent())
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOnlyAlertOnce(true)
            .setOngoing(status in ACTIVE_STATUSES)
            .setAutoCancel(status !in ACTIVE_STATUSES)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        if (status == AudioDownloadStatus.DOWNLOADING) {
            if (totalBytes > 0L) {
                val progress = ((downloadedBytes.toDouble() / totalBytes.toDouble()) * PROGRESS_MAX)
                    .toInt()
                    .coerceIn(0, PROGRESS_MAX)
                builder.setProgress(PROGRESS_MAX, progress, false)
            } else {
                builder.setProgress(0, 0, true)
            }
        }
        when (status) {
            AudioDownloadStatus.QUEUED,
            AudioDownloadStatus.RESOLVING,
            AudioDownloadStatus.DOWNLOADING,
            -> {
                builder.addAction(
                    android.R.drawable.ic_media_pause,
                    "暂停",
                    actionPendingIntent(ACTION_PAUSE, taskId),
                )
                builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "取消",
                    actionPendingIntent(ACTION_CANCEL, taskId),
                )
            }
            AudioDownloadStatus.PAUSED,
            AudioDownloadStatus.FAILED,
            -> {
                builder.addAction(
                    android.R.drawable.ic_media_play,
                    if (status == AudioDownloadStatus.PAUSED) "继续" else "重试",
                    actionPendingIntent(ACTION_RESUME, taskId),
                )
                builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "取消",
                    actionPendingIntent(ACTION_CANCEL, taskId),
                )
            }
            AudioDownloadStatus.CANCELLED -> builder.addAction(
                android.R.drawable.ic_media_play,
                "重试",
                actionPendingIntent(ACTION_RESUME, taskId),
            )
            AudioDownloadStatus.PUBLISHING,
            AudioDownloadStatus.COMPLETED,
            -> Unit
        }
        return builder.build()
    }

    private fun actionPendingIntent(action: String, taskId: String): PendingIntent {
        val intent = Intent(this, AudioDownloadService::class.java)
            .setAction(action)
            .putExtra(EXTRA_TASK_ID, taskId)
        return PendingIntent.getForegroundService(
            this,
            actionRequestCode(action, taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            this,
            OPEN_APP_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun startAsForeground(notificationId: Int, notification: Notification) {
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(this, notificationId, notification, foregroundType)
    }

    private fun notifySafely(notificationId: Int, notification: Notification) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        runCatching { NotificationManagerCompat.from(this).notify(notificationId, notification) }
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "音频下载",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "显示 Biu 音频下载进度和控制按钮"
            },
        )
    }

    private fun tempFile(taskId: String): File {
        return DownloadTempFilePolicy.audio(filesDir, taskId)
    }

    private fun stopForegroundAndSelfIfIdle() {
        if (activeJob?.isActive == true || waitingForNetwork) return
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun requestFromIntent(intent: Intent): AudioDownloadRequest? {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID)?.takeIf(String::isNotBlank) ?: return null
        val bvid = intent.getStringExtra(EXTRA_BVID)?.takeIf(String::isNotBlank) ?: return null
        val cid = intent.getLongExtra(EXTRA_CID, 0L).takeIf { it > 0L } ?: return null
        return AudioDownloadRequest(
            taskId = taskId,
            bvid = bvid,
            cid = cid,
            title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { bvid },
            artist = intent.getStringExtra(EXTRA_ARTIST).orEmpty().ifBlank { "未知艺术家" },
            artworkUrl = intent.getStringExtra(EXTRA_ARTWORK_URL),
            qualityPreference = runCatching {
                com.lonnnnnng.biu.core.model.AudioQualityPreference.valueOf(
                    intent.getStringExtra(EXTRA_QUALITY_PREFERENCE).orEmpty(),
                )
            }.getOrDefault(com.lonnnnnng.biu.core.model.AudioQualityPreference.HIGHEST),
        )
    }

    private fun safeErrorMessage(error: Throwable): String {
        return when {
            error.message == "暂不支持该音频编码" -> error.message.orEmpty()
            error.message?.contains("空间", ignoreCase = true) == true -> "存储空间不足，无法完成下载"
            else -> "下载失败，请检查网络后重试"
        }
    }

    companion object {
        fun start(context: Context, request: AudioDownloadRequest) {
            val intent = Intent(context, AudioDownloadService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_TASK_ID, request.taskId)
                .putExtra(EXTRA_BVID, request.bvid)
                .putExtra(EXTRA_CID, request.cid)
                .putExtra(EXTRA_TITLE, request.title)
                .putExtra(EXTRA_ARTIST, request.artist)
                .putExtra(EXTRA_ARTWORK_URL, request.artworkUrl)
                .putExtra(EXTRA_QUALITY_PREFERENCE, request.qualityPreference.name)
            ContextCompat.startForegroundService(context, intent)
        }

        fun resume(context: Context, taskId: String) = sendAction(context, ACTION_RESUME, taskId)

        fun pause(context: Context, taskId: String) = sendAction(context, ACTION_PAUSE, taskId)

        fun cancel(context: Context, taskId: String) = sendAction(context, ACTION_CANCEL, taskId)

        fun drain(context: Context) = sendAction(context, ACTION_DRAIN, "")

        fun refreshNetworkConstraint(context: Context) = sendAction(context, ACTION_REFRESH_NETWORK, "")

        private fun sendAction(context: Context, action: String, taskId: String) {
            val intent = Intent(context, AudioDownloadService::class.java)
                .setAction(action)
                .putExtra(EXTRA_TASK_ID, taskId)
            ContextCompat.startForegroundService(context, intent)
        }

        private const val ACTION_START = "com.lonnnnnng.biu.download.START"
        private const val ACTION_RESUME = "com.lonnnnnng.biu.download.RESUME"
        private const val ACTION_PAUSE = "com.lonnnnnng.biu.download.PAUSE"
        private const val ACTION_CANCEL = "com.lonnnnnng.biu.download.CANCEL"
        private const val ACTION_DRAIN = "com.lonnnnnng.biu.download.DRAIN"
        private const val ACTION_REFRESH_NETWORK = "com.lonnnnnng.biu.download.REFRESH_NETWORK"
        private const val EXTRA_TASK_ID = "task_id"
        private const val EXTRA_BVID = "bvid"
        private const val EXTRA_CID = "cid"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_ARTIST = "artist"
        private const val EXTRA_ARTWORK_URL = "artwork_url"
        private const val EXTRA_QUALITY_PREFERENCE = "quality_preference"
    }
}

private enum class StopReason {
    PAUSE,
    CANCEL,
    NETWORK,
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kib = bytes / 1024.0
    if (kib < 1024.0) return "%.1f KB".format(kib)
    return "%.1f MB".format(kib / 1024.0)
}

private fun notificationId(taskId: String): Int {
    return NOTIFICATION_ID_BASE + (taskId.hashCode().toLong().absoluteValue % NOTIFICATION_ID_RANGE).toInt()
}

private fun actionRequestCode(action: String, taskId: String): Int {
    return ("$action:$taskId".hashCode().toLong().absoluteValue % Int.MAX_VALUE).toInt()
}

private val ACTIVE_STATUSES = setOf(
    AudioDownloadStatus.QUEUED,
    AudioDownloadStatus.RESOLVING,
    AudioDownloadStatus.DOWNLOADING,
    AudioDownloadStatus.PUBLISHING,
)
private const val LOG_TAG = "BiuDownload"
private const val NOTIFICATION_CHANNEL_ID = "audio_downloads"
private const val NOTIFICATION_ID_BASE = 20_000
private const val NOTIFICATION_ID_RANGE = 10_000
private const val OPEN_APP_REQUEST_CODE = 31_001
private const val PROGRESS_MAX = 1_000
private const val PROGRESS_PERSIST_BYTES = 512 * 1024L
private val PROGRESS_PERSIST_NANOS = TimeUnit.SECONDS.toNanos(1)

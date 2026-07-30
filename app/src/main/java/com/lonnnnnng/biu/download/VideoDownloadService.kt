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
import androidx.media3.common.util.UnstableApi
import com.lonnnnnng.biu.MainActivity
import com.lonnnnnng.biu.appContainer
import com.lonnnnnng.biu.data.local.VideoDownloadTaskEntity
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

class VideoDownloadService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val commandMutex = Mutex()
    private val repository by lazy { appContainer.videoDownloadRepository }
    private val bilibiliRepository by lazy { appContainer.bilibiliRepository }
    private val downloader by lazy { ResumableFileDownloader(appContainer.bilibiliHttpClient) }
    private val muxer by lazy(::VideoTrackMuxer)
    private val publisher by lazy { VideoDownloadPublisher(this) }

    @Volatile
    private var activeTaskId: String? = null

    @Volatile
    private var requestedStop: VideoStopReason? = null

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
            val bootstrapTitle = request?.title ?: "视频下载"
            startAsForeground(
                notificationId(taskId.ifBlank { bootstrapTitle }),
                buildNotification(
                    taskId = taskId,
                    title = bootstrapTitle,
                    text = "正在准备视频下载",
                    status = VideoDownloadStatus.RESOLVING,
                    downloadedBytes = 0L,
                    totalBytes = 0L,
                ),
            )
        }
        serviceScope.launch {
            // long: 先完成进程遗留任务的暂停恢复，再接受新命令，避免冷启动恢复写入覆盖用户刚触发的状态。
            appContainer.videoDownloadRecovery.await()
            commandMutex.withLock {
                runCatching {
                    when (action) {
                        ACTION_START -> request?.let { enqueue(it) }
                        ACTION_RESUME -> resume(taskId)
                        ACTION_PAUSE -> pause(taskId)
                        ACTION_CANCEL -> cancelTask(taskId)
                    }
                }.onFailure { error ->
                    Log.w(LOG_TAG, "Video download command failed: ${error.javaClass.simpleName}")
                    stopForegroundAndSelfIfIdle()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        downloader.cancelActiveCall()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        val taskId = activeTaskId
        requestedStop = VideoStopReason.PAUSE
        downloader.cancelActiveCall()
        activeJob?.cancel()
        if (!taskId.isNullOrBlank()) {
            // long: Android 15+ dataSync 配额结束后同步保存暂停态，让已下载的两条轨能从断点继续而不是永久显示运行中。
            runCatching {
                runBlocking(Dispatchers.IO) {
                    transitionIfAllowed(taskId, VideoDownloadStatus.PAUSED)
                }
            }
        }
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf(startId)
    }

    private suspend fun enqueue(request: VideoDownloadRequest) {
        val paths = tempFiles(request.taskId)
        val task = repository.enqueue(
            request = request,
            videoTempFilePath = paths.video.absolutePath,
            audioTempFilePath = paths.audio.absolutePath,
            outputTempFilePath = paths.output.absolutePath,
        )
        when (task.downloadStatus) {
            VideoDownloadStatus.COMPLETED -> {
                postTaskNotification(task, "已保存到 Movies/Biu", active = false)
                stopForegroundAndSelfIfIdle()
            }
            VideoDownloadStatus.QUEUED -> {
                if (activeJob?.isActive == true) {
                    postTaskNotification(task, "已加入视频下载队列", active = false)
                } else {
                    launchNextQueued()
                }
            }
            else -> Unit
        }
    }

    private suspend fun resume(taskId: String) {
        val existing = repository.find(taskId) ?: return stopForegroundAndSelfIfIdle()
        val task = repository.enqueue(
            request = existing.toRequest(),
            videoTempFilePath = existing.videoTempFilePath,
            audioTempFilePath = existing.audioTempFilePath,
            outputTempFilePath = existing.outputTempFilePath,
        )
        if (task.downloadStatus == VideoDownloadStatus.COMPLETED) {
            postTaskNotification(task, "已保存到 Movies/Biu", active = false)
            return stopForegroundAndSelfIfIdle()
        }
        if (activeJob?.isActive == true) {
            postTaskNotification(task, "已加入视频下载队列", active = false)
        } else {
            launchNextQueued()
        }
    }

    private suspend fun pause(taskId: String) {
        if (taskId == activeTaskId) {
            requestedStop = VideoStopReason.PAUSE
            downloader.cancelActiveCall()
            activeJob?.cancel()
            return
        }
        val task = repository.find(taskId) ?: return stopForegroundAndSelfIfIdle()
        val paused = transitionIfAllowed(task.taskId, VideoDownloadStatus.PAUSED)
            ?: return stopForegroundAndSelfIfIdle()
        postTaskNotification(paused, "视频下载已暂停", active = false)
        stopForegroundAndSelfIfIdle()
    }

    private suspend fun cancelTask(taskId: String) {
        if (taskId == activeTaskId) {
            requestedStop = VideoStopReason.CANCEL
            downloader.cancelActiveCall()
            activeJob?.cancel()
            return
        }
        val task = repository.find(taskId) ?: return stopForegroundAndSelfIfIdle()
        val cancelled = transitionIfAllowed(task.taskId, VideoDownloadStatus.CANCELLED)
            ?: return stopForegroundAndSelfIfIdle()
        clearCancelledTask(cancelled)
        stopForegroundAndSelfIfIdle()
    }

    private suspend fun launchNextQueued() {
        if (activeJob?.isActive == true) return
        val next = repository.nextQueued() ?: return stopForegroundAndSelfIfIdle()
        activeTaskId = next.taskId
        requestedStop = null
        activeJob = serviceScope.launch { runTask(next.taskId) }
    }

    @androidx.annotation.OptIn(markerClass = [UnstableApi::class])
    private suspend fun runTask(taskId: String) {
        var completed = false
        try {
            var task = repository.transition(taskId, VideoDownloadStatus.RESOLVING) ?: return
            postTaskNotification(task, "正在解析 DASH 音视频轨", active = true)
            val streams = bilibiliRepository.resolveVideoDownloadStreams(task.bvid, task.cid)
            if (!streams.audio.codecs.contains("mp4a", ignoreCase = true)) {
                throw IOException("暂不支持该音频编码")
            }
            task = repository.transition(
                taskId = taskId,
                target = VideoDownloadStatus.DOWNLOADING_VIDEO,
                videoQualityLabel = streams.video.qualityLabel,
                audioQualityLabel = streams.audio.qualityLabel,
            ) ?: return
            postTaskNotification(task, "正在下载视频轨 · ${streams.video.qualityLabel}", active = true)
            val videoFile = File(task.videoTempFilePath)
            val videoBytes = downloadTrack(
                task = task,
                stage = VideoDownloadStatus.DOWNLOADING_VIDEO,
                urls = listOf(streams.video.url) + streams.video.backupUrls,
                targetFile = videoFile,
            )
            repository.updateVideoProgress(taskId, videoBytes, videoBytes)

            task = repository.transition(taskId, VideoDownloadStatus.DOWNLOADING_AUDIO) ?: return
            postTaskNotification(task, "正在下载音频轨 · ${streams.audio.qualityLabel}", active = true)
            val audioFile = File(task.audioTempFilePath)
            val audioBytes = downloadTrack(
                task = task,
                stage = VideoDownloadStatus.DOWNLOADING_AUDIO,
                urls = listOf(streams.audio.url) + streams.audio.backupUrls,
                targetFile = audioFile,
            )
            repository.updateAudioProgress(taskId, audioBytes, audioBytes)

            task = repository.transition(taskId, VideoDownloadStatus.MUXING) ?: return
            postTaskNotification(task, "正在无损合并音视频轨", active = true)
            val outputFile = File(task.outputTempFilePath)
            val outputBytes = muxer.mux(videoFile, audioFile, outputFile)

            task = repository.transition(taskId, VideoDownloadStatus.PUBLISHING) ?: return
            postTaskNotification(task, "正在保存到 Movies/Biu", active = true)
            val publishedUri = publisher.publish(task.toRequest(), outputFile)
            repository.markCompleted(taskId, publishedUri.toString(), outputBytes)
            videoFile.delete()
            audioFile.delete()
            outputFile.delete()
            val finished = repository.find(taskId) ?: return
            postTaskNotification(finished, "视频下载完成 · ${finished.videoQualityLabel}", active = false)
            completed = true
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { applyRequestedStop(taskId) }
        } catch (error: Throwable) {
            withContext(NonCancellable) {
                if (requestedStop != null) {
                    applyRequestedStop(taskId)
                } else {
                    Log.w(LOG_TAG, "Video download task failed: ${error.javaClass.simpleName}")
                    val failed = transitionIfAllowed(
                        taskId = taskId,
                        target = VideoDownloadStatus.FAILED,
                        errorMessage = safeErrorMessage(error),
                    )
                    failed?.let { postTaskNotification(it, it.errorMessage ?: "视频下载失败，请重试", active = false) }
                }
            }
        } finally {
            withContext(NonCancellable) {
                activeTaskId = null
                requestedStop = null
                activeJob = null
                if (completed) stopForeground(STOP_FOREGROUND_DETACH)
                commandMutex.withLock { launchNextQueued() }
            }
        }
    }

    private suspend fun downloadTrack(
        task: VideoDownloadTaskEntity,
        stage: VideoDownloadStatus,
        urls: List<String>,
        targetFile: File,
    ): Long {
        val knownTotalBytes = when (stage) {
            VideoDownloadStatus.DOWNLOADING_VIDEO -> task.videoTotalBytes
            VideoDownloadStatus.DOWNLOADING_AUDIO -> task.audioTotalBytes
            else -> 0L
        }
        if (knownTotalBytes > 0L && targetFile.isFile && targetFile.length() == knownTotalBytes) {
            return knownTotalBytes
        }
        var lastPersistedBytes = targetFile.takeIf(File::isFile)?.length() ?: 0L
        var lastPersistedAtNanos = System.nanoTime()
        val downloadedBytes = downloader.download(urls, targetFile) { bytes, total ->
            val nowNanos = System.nanoTime()
            if (
                bytes == total ||
                bytes < lastPersistedBytes ||
                bytes - lastPersistedBytes >= PROGRESS_PERSIST_BYTES ||
                nowNanos - lastPersistedAtNanos >= PROGRESS_PERSIST_NANOS
            ) {
                when (stage) {
                    VideoDownloadStatus.DOWNLOADING_VIDEO -> repository.updateVideoProgress(task.taskId, bytes, total)
                    VideoDownloadStatus.DOWNLOADING_AUDIO -> repository.updateAudioProgress(task.taskId, bytes, total)
                    else -> Unit
                }
                postProgressNotification(task, stage, bytes, total)
                lastPersistedBytes = bytes
                lastPersistedAtNanos = nowNanos
            }
        }
        return downloadedBytes
    }

    private suspend fun applyRequestedStop(taskId: String) {
        when (requestedStop) {
            VideoStopReason.PAUSE -> {
                val paused = transitionIfAllowed(taskId, VideoDownloadStatus.PAUSED)
                paused?.let { postTaskNotification(it, "视频下载已暂停", active = false) }
            }
            VideoStopReason.CANCEL -> {
                val cancelled = transitionIfAllowed(taskId, VideoDownloadStatus.CANCELLED)
                cancelled?.let { task -> clearCancelledTask(task) }
            }
            null -> Unit
        }
        stopForeground(STOP_FOREGROUND_DETACH)
    }

    private suspend fun clearCancelledTask(task: VideoDownloadTaskEntity) {
        File(task.videoTempFilePath).delete()
        File(task.audioTempFilePath).delete()
        File(task.outputTempFilePath).delete()
        // long: 取消会清除两条轨和合并文件，Room 进度同步归零，任务面板不能继续展示已经不存在的断点。
        repository.updateVideoProgress(task.taskId, 0L, 0L)
        repository.updateAudioProgress(task.taskId, 0L, 0L)
        val cleared = repository.find(task.taskId) ?: task.copy(
            videoDownloadedBytes = 0L,
            videoTotalBytes = 0L,
            audioDownloadedBytes = 0L,
            audioTotalBytes = 0L,
        )
        postTaskNotification(cleared, "视频下载已取消", active = false)
    }

    private suspend fun transitionIfAllowed(
        taskId: String,
        target: VideoDownloadStatus,
        errorMessage: String? = null,
    ): VideoDownloadTaskEntity? {
        val current = repository.find(taskId) ?: return null
        if (current.downloadStatus == target) return current
        if (!VideoDownloadStatePolicy.canTransition(current.downloadStatus, target)) {
            // long: 合并发布完成与用户点击控制按钮可能同时发生；拒绝迟到命令可防止已发布视频被错误标记或清空轨道进度。
            return null
        }
        return repository.transition(taskId, target, errorMessage = errorMessage)
    }

    private fun postProgressNotification(
        task: VideoDownloadTaskEntity,
        stage: VideoDownloadStatus,
        downloadedBytes: Long,
        totalBytes: Long,
    ) {
        val stageLabel = if (stage == VideoDownloadStatus.DOWNLOADING_VIDEO) "视频轨" else "音频轨"
        val text = if (totalBytes > 0L) {
            "$stageLabel · ${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}"
        } else {
            "$stageLabel · ${formatBytes(downloadedBytes)}"
        }
        startAsForeground(
            notificationId(task.taskId),
            buildNotification(task.taskId, task.title, text, stage, downloadedBytes, totalBytes),
        )
    }

    private fun postTaskNotification(task: VideoDownloadTaskEntity, text: String, active: Boolean) {
        val notification = buildNotification(
            taskId = task.taskId,
            title = task.title,
            text = text,
            status = task.downloadStatus,
            downloadedBytes = task.videoDownloadedBytes + task.audioDownloadedBytes,
            totalBytes = task.videoTotalBytes + task.audioTotalBytes,
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
        status: VideoDownloadStatus,
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
        if (status in DOWNLOAD_STATUSES) {
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
            VideoDownloadStatus.QUEUED,
            VideoDownloadStatus.RESOLVING,
            VideoDownloadStatus.DOWNLOADING_VIDEO,
            VideoDownloadStatus.DOWNLOADING_AUDIO,
            VideoDownloadStatus.MUXING,
            VideoDownloadStatus.PUBLISHING,
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
            VideoDownloadStatus.PAUSED,
            VideoDownloadStatus.FAILED,
            -> {
                builder.addAction(
                    android.R.drawable.ic_media_play,
                    if (status == VideoDownloadStatus.PAUSED) "继续" else "重试",
                    actionPendingIntent(ACTION_RESUME, taskId),
                )
                builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "取消",
                    actionPendingIntent(ACTION_CANCEL, taskId),
                )
            }
            VideoDownloadStatus.CANCELLED -> builder.addAction(
                android.R.drawable.ic_media_play,
                "重试",
                actionPendingIntent(ACTION_RESUME, taskId),
            )
            VideoDownloadStatus.COMPLETED -> Unit
        }
        return builder.build()
    }

    private fun actionPendingIntent(action: String, taskId: String): PendingIntent {
        val intent = Intent(this, VideoDownloadService::class.java)
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
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "视频下载",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "显示 Biu 视频双轨下载、合并进度和控制按钮"
            },
        )
    }

    private fun tempFiles(taskId: String): VideoTempFiles {
        val safeId = taskId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val directory = File(filesDir, "video-downloads")
        return VideoTempFiles(
            video = File(directory, "$safeId.video.m4s"),
            audio = File(directory, "$safeId.audio.m4s"),
            output = File(directory, "$safeId.output.mp4"),
        )
    }

    private fun stopForegroundAndSelfIfIdle() {
        if (activeJob?.isActive == true) return
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun requestFromIntent(intent: Intent): VideoDownloadRequest? {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID)?.takeIf(String::isNotBlank) ?: return null
        val bvid = intent.getStringExtra(EXTRA_BVID)?.takeIf(String::isNotBlank) ?: return null
        val cid = intent.getLongExtra(EXTRA_CID, 0L).takeIf { it > 0L } ?: return null
        return VideoDownloadRequest(
            taskId = taskId,
            bvid = bvid,
            cid = cid,
            title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { bvid },
            artist = intent.getStringExtra(EXTRA_ARTIST).orEmpty().ifBlank { "未知作者" },
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
            error.message?.contains("轨") == true || error.message?.contains("合并") == true ->
                "视频轨格式不兼容，暂时无法合并"
            error.message?.contains("空间", ignoreCase = true) == true -> "存储空间不足，无法完成视频下载"
            else -> "视频下载失败，请检查网络后重试"
        }
    }

    companion object {
        fun start(context: Context, request: VideoDownloadRequest) {
            val intent = Intent(context, VideoDownloadService::class.java)
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

        private fun sendAction(context: Context, action: String, taskId: String) {
            val intent = Intent(context, VideoDownloadService::class.java)
                .setAction(action)
                .putExtra(EXTRA_TASK_ID, taskId)
            ContextCompat.startForegroundService(context, intent)
        }

        private const val ACTION_START = "com.lonnnnnng.biu.download.VIDEO_START"
        private const val ACTION_RESUME = "com.lonnnnnng.biu.download.VIDEO_RESUME"
        private const val ACTION_PAUSE = "com.lonnnnnng.biu.download.VIDEO_PAUSE"
        private const val ACTION_CANCEL = "com.lonnnnnng.biu.download.VIDEO_CANCEL"
        private const val EXTRA_TASK_ID = "task_id"
        private const val EXTRA_BVID = "bvid"
        private const val EXTRA_CID = "cid"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_ARTIST = "artist"
        private const val EXTRA_ARTWORK_URL = "artwork_url"
        private const val EXTRA_QUALITY_PREFERENCE = "quality_preference"
    }
}

private data class VideoTempFiles(
    val video: File,
    val audio: File,
    val output: File,
)

private enum class VideoStopReason {
    PAUSE,
    CANCEL,
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
    VideoDownloadStatus.QUEUED,
    VideoDownloadStatus.RESOLVING,
    VideoDownloadStatus.DOWNLOADING_VIDEO,
    VideoDownloadStatus.DOWNLOADING_AUDIO,
    VideoDownloadStatus.MUXING,
    VideoDownloadStatus.PUBLISHING,
)
private val DOWNLOAD_STATUSES = setOf(
    VideoDownloadStatus.DOWNLOADING_VIDEO,
    VideoDownloadStatus.DOWNLOADING_AUDIO,
)
private const val LOG_TAG = "BiuVideoDownload"
private const val NOTIFICATION_CHANNEL_ID = "video_downloads"
private const val NOTIFICATION_ID_BASE = 40_000
private const val NOTIFICATION_ID_RANGE = 10_000
private const val OPEN_APP_REQUEST_CODE = 51_001
private const val PROGRESS_MAX = 1_000
private const val PROGRESS_PERSIST_BYTES = 512 * 1024L
private val PROGRESS_PERSIST_NANOS = TimeUnit.SECONDS.toNanos(1)

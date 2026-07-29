package com.lonnnnnng.biu.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.lonnnnnng.biu.data.update.AppUpdate

class AppUpdateInstaller(context: Context) {
    private val appContext = context.applicationContext
    private val downloadManager = appContext.getSystemService(DownloadManager::class.java)
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun enqueue(update: AppUpdate): Long {
        val safeVersion = update.version.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val fileName = "BiuApp-$safeVersion-${System.currentTimeMillis()}.apk"
        val request = DownloadManager.Request(Uri.parse(update.downloadUrl))
            .setMimeType(APK_MIME_TYPE)
            .setTitle("BiuApp ${update.version}")
            .setDescription("正在下载应用更新")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(appContext, Environment.DIRECTORY_DOWNLOADS, fileName)
        // long: 下载交由系统服务托管，即使 Biu 进程退出，断点重试和完成通知仍由系统继续处理。
        return downloadManager.enqueue(request).also { downloadId ->
            preferences.edit().putLong(KEY_PENDING_DOWNLOAD_ID, downloadId).apply()
        }
    }

    fun pendingDownloadId(): Long = preferences.getLong(KEY_PENDING_DOWNLOAD_ID, -1L)

    fun downloadState(downloadId: Long): DownloadState {
        downloadManager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
            if (!cursor.moveToFirst()) return DownloadState.Missing
            return when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_SUCCESSFUL -> DownloadState.Successful
                DownloadManager.STATUS_FAILED -> {
                    val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    DownloadState.Failed("更新包下载失败（错误码 $reason）")
                }
                else -> DownloadState.Pending
            }
        }
    }

    fun clearPendingDownload(downloadId: Long) {
        if (pendingDownloadId() == downloadId) {
            preferences.edit().remove(KEY_PENDING_DOWNLOAD_ID).apply()
        }
    }

    fun installDownloaded(downloadId: Long): InstallResult {
        if (downloadState(downloadId) != DownloadState.Successful) {
            return InstallResult.Failed("更新包尚未下载完成")
        }
        if (!appContext.packageManager.canRequestPackageInstalls()) {
            return InstallResult.PermissionRequired
        }
        val uri = downloadManager.getUriForDownloadedFile(downloadId)
            ?: return InstallResult.Failed("无法读取已下载的更新包")
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, APK_MIME_TYPE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
            clearPendingDownload(downloadId)
            InstallResult.Started
        }.getOrElse { error ->
            InstallResult.Failed(error.message?.takeIf(String::isNotBlank) ?: "无法打开系统安装界面")
        }
    }

    sealed interface InstallResult {
        data object Started : InstallResult
        data object PermissionRequired : InstallResult
        data class Failed(val message: String) : InstallResult
    }

    sealed interface DownloadState {
        data object Pending : DownloadState
        data object Successful : DownloadState
        data object Missing : DownloadState
        data class Failed(val message: String) : DownloadState
    }

    private companion object {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        const val PREFERENCES_NAME = "app_update"
        const val KEY_PENDING_DOWNLOAD_ID = "pending_download_id"
    }
}

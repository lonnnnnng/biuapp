package com.lonnnnnng.biu.download

internal class DownloadCompletionNotificationCoordinator(
    private val postCompletedNotification: (() -> Unit)?,
    private val removeForegroundNotification: () -> Unit,
) {
    fun complete() {
        // long: 完成状态落库后必须先移除系统持有的进度通知；即使终态记录暂时查不到或通知权限受限，也不能遗留“正在下载”。
        removeForegroundNotification()
        postCompletedNotification?.invoke()
    }
}

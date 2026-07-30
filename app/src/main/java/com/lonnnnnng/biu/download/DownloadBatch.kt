package com.lonnnnnng.biu.download

data class DownloadBatchEnqueueResult(
    val requestedCount: Int,
    val queuedCount: Int,
    val skippedCompletedCount: Int,
    val skippedExistingCount: Int,
)

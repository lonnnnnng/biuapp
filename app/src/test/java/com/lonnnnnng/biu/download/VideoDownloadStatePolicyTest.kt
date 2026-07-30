package com.lonnnnnng.biu.download

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoDownloadStatePolicyTest {
    @Test
    fun `视频任务按双轨下载合并发布顺序推进并允许恢复`() {
        val expected = mapOf(
            VideoDownloadStatus.QUEUED to setOf(
                VideoDownloadStatus.RESOLVING,
                VideoDownloadStatus.PAUSED,
                VideoDownloadStatus.CANCELLED,
            ),
            VideoDownloadStatus.RESOLVING to runningTargets(VideoDownloadStatus.DOWNLOADING_VIDEO),
            VideoDownloadStatus.DOWNLOADING_VIDEO to runningTargets(VideoDownloadStatus.DOWNLOADING_AUDIO),
            VideoDownloadStatus.DOWNLOADING_AUDIO to runningTargets(VideoDownloadStatus.MUXING),
            VideoDownloadStatus.MUXING to runningTargets(VideoDownloadStatus.PUBLISHING),
            VideoDownloadStatus.PUBLISHING to runningTargets(VideoDownloadStatus.COMPLETED),
            VideoDownloadStatus.PAUSED to setOf(VideoDownloadStatus.QUEUED, VideoDownloadStatus.CANCELLED),
            VideoDownloadStatus.COMPLETED to emptySet(),
            VideoDownloadStatus.FAILED to setOf(VideoDownloadStatus.QUEUED, VideoDownloadStatus.CANCELLED),
            VideoDownloadStatus.CANCELLED to setOf(VideoDownloadStatus.QUEUED),
        )

        val actual = VideoDownloadStatus.entries.associateWith(VideoDownloadStatePolicy::allowedTargets)

        assertEquals(expected, actual)
    }

    private fun runningTargets(next: VideoDownloadStatus): Set<VideoDownloadStatus> {
        return setOf(next, VideoDownloadStatus.PAUSED, VideoDownloadStatus.FAILED, VideoDownloadStatus.CANCELLED)
    }
}

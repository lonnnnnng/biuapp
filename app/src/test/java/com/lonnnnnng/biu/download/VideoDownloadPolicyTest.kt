package com.lonnnnnng.biu.download

import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoDownloadPolicyTest {
    @Test
    fun `当前分 P 生成稳定任务并发布到 Biu 视频目录`() {
        val request = VideoDownloadRequest.create(
            source = BilibiliTrackSource("BV1-video", 88002L),
            currentTitle = "002. 海阔天空/Beyond",
            resourceTitle = "经典歌曲合集",
            artist = "音乐收藏家?",
            artworkUrl = "https://example.invalid/cover.jpg",
        )

        val spec = VideoDownloadPublishPolicy.spec(request, sdkInt = 29)

        assertEquals("BV1-video:88002", request.taskId)
        assertEquals("002. 海阔天空/Beyond", request.title)
        assertEquals("002. 海阔天空_Beyond - 音乐收藏家_.mp4", request.displayName)
        assertEquals("video/mp4", spec.mimeType)
        assertEquals("Movies/Biu/", spec.relativePath)
        assertTrue(spec.isPending)
    }
}

package com.lonnnnnng.biu.download

import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioDownloadPolicyTest {
    @Test
    fun currentPageCreatesStableDownloadRequestAndSafeM4aName() {
        val request = AudioDownloadRequest.create(
            source = BilibiliTrackSource("BV1-test", 22002L),
            currentTitle = "海阔天空/Live",
            resourceTitle = "音乐合集",
            artist = "Beyond?",
            artworkUrl = "https://example.invalid/cover.jpg",
        )

        assertEquals("BV1-test:22002", request.taskId)
        assertEquals("海阔天空/Live", request.title)
        assertEquals("海阔天空_Live - Beyond_.m4a", request.displayName)
        assertEquals("https://example.invalid/cover.jpg", request.artworkUrl)
    }
}

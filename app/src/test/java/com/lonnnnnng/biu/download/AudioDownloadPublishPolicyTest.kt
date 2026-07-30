package com.lonnnnnng.biu.download

import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioDownloadPublishPolicyTest {
    @Test
    fun androidTenPublishesPendingM4aIntoBiuMusicDirectory() {
        val request = AudioDownloadRequest.create(
            source = BilibiliTrackSource("BV1-publish", 8899L),
            currentTitle = "夜曲",
            resourceTitle = "十一月的萧邦",
            artist = "周杰伦",
            artworkUrl = null,
        )

        val spec = AudioDownloadPublishPolicy.spec(request, sdkInt = 29)

        assertEquals("夜曲 - 周杰伦.m4a", spec.displayName)
        assertEquals("audio/mp4", spec.mimeType)
        assertEquals("Music/Biu/", spec.relativePath)
        assertEquals("夜曲", spec.title)
        assertEquals("周杰伦", spec.artist)
        assertTrue(spec.isPending)
    }
}

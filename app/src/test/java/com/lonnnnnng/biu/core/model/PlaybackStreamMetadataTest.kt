package com.lonnnnnng.biu.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackStreamMetadataTest {
    @Test
    fun `新媒体默认使用音频模式`() {
        assertEquals(PlaybackMediaMode.AUDIO, PlaybackMediaMode.fromStoredValue(null))
        assertEquals(PlaybackMediaMode.AUDIO, PlaybackMediaMode.fromStoredValue("unknown"))
    }

    @Test
    fun `视频模式播放视频地址但持久化音频地址`() {
        val qualities = listOf(
            PlaybackVideoQuality(qualityId = 80, label = "1080p · AVC"),
            PlaybackVideoQuality(qualityId = 64, label = "720p · AVC"),
        )
        val metadata = PlaybackStreamMetadata.video(
            audioUrl = "https://cdn.example/audio.m4s",
            videoUrl = "https://cdn.example/video.m4s",
            audioQualityLabel = "192 kbps",
            videoQualityLabel = "1080p · AVC",
            selectedVideoQualityId = 80,
            availableVideoQualities = qualities,
        )

        assertEquals(PlaybackMediaMode.VIDEO, metadata.mode)
        assertEquals("https://cdn.example/video.m4s", metadata.playbackUrl)
        assertEquals("https://cdn.example/audio.m4s", metadata.persistentAudioUrl)
        assertEquals("1080p · AVC", metadata.displayQualityLabel)
        assertEquals(80, metadata.selectedVideoQualityId)
        assertEquals(qualities, metadata.availableVideoQualities)
    }

    @Test
    fun `音频模式不携带视频地址`() {
        val metadata = PlaybackStreamMetadata.audio(
            audioUrl = "https://cdn.example/audio.m4s",
            qualityLabel = "192 kbps",
        )

        assertEquals(PlaybackMediaMode.AUDIO, metadata.mode)
        assertEquals(metadata.audioUrl, metadata.playbackUrl)
        assertEquals(metadata.audioUrl, metadata.persistentAudioUrl)
        assertEquals("192 kbps", metadata.displayQualityLabel)
        assertNull(metadata.videoUrl)
        assertNull(metadata.selectedVideoQualityId)
        assertEquals(emptyList<PlaybackVideoQuality>(), metadata.availableVideoQualities)
    }
}

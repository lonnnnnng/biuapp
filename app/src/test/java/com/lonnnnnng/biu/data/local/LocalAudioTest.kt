package com.lonnnnnng.biu.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalAudioTest {
    @Test
    fun `MediaStore 标题为空时使用去扩展名的文件名`() {
        assertEquals(
            "夜曲",
            LocalAudioMetadataPolicy.title(title = "", displayName = "夜曲.mp3"),
        )
    }

    @Test
    fun `MediaStore 未知艺术家显示中文兜底`() {
        assertEquals("未知艺术家", LocalAudioMetadataPolicy.artist("<unknown>"))
        assertEquals("未知艺术家", LocalAudioMetadataPolicy.artist(""))
    }

    @Test
    fun `本地音频映射为 content Uri 播放音轨`() {
        val track = LocalAudio(
            mediaStoreId = 42L,
            title = "夜曲",
            artist = "周杰伦",
            album = "十一月的萧邦",
            durationMs = 226_000L,
            contentUri = "content://media/external/audio/media/42",
        ).toTrack()

        assertEquals("local:42", track.id)
        assertEquals("夜曲", track.title)
        assertEquals("周杰伦", track.artist)
        assertEquals("content://media/external/audio/media/42", track.streamUrl)
        assertEquals("本地音频", track.qualityLabel)
        assertNull(track.source)
    }
}

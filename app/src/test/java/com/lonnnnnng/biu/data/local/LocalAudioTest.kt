package com.lonnnnnng.biu.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalAudioTest {
    private val audio = listOf(
        localAudio(id = 1L, title = "夜曲"),
        localAudio(id = 2L, title = "海阔天空"),
        localAudio(id = 3L, title = "无赖"),
    )

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

    @Test
    fun `仅播放此曲时队列只包含当前歌曲`() {
        val plan = LocalAudioPlaybackPolicy.create(audio, 2L, LocalAudioPlaybackMode.SINGLE)

        assertEquals(listOf(2L), plan?.audio?.map(LocalAudio::mediaStoreId))
        assertEquals(0, plan?.startIndex)
    }

    @Test
    fun `播放当前目录时保留列表顺序和当前索引`() {
        val plan = LocalAudioPlaybackPolicy.create(audio, 2L, LocalAudioPlaybackMode.CURRENT_DIRECTORY)

        assertEquals(listOf(1L, 2L, 3L), plan?.audio?.map(LocalAudio::mediaStoreId))
        assertEquals(1, plan?.startIndex)
    }

    @Test
    fun `当前歌曲已不在扫描结果时不创建播放计划`() {
        assertNull(LocalAudioPlaybackPolicy.create(audio, 9L, LocalAudioPlaybackMode.CURRENT_DIRECTORY))
    }

    private fun localAudio(id: Long, title: String) = LocalAudio(
        mediaStoreId = id,
        title = title,
        artist = "测试歌手",
        album = "测试专辑",
        durationMs = 180_000L,
        contentUri = "content://media/external/audio/media/$id",
    )
}

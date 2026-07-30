package com.lonnnnnng.biu.data.local

import com.lonnnnnng.biu.download.AudioDownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalAudioDownloadMetadataPolicyTest {
    @Test
    fun completedDownloadRestoresMetadataOverMediaStoreScanFallbacks() {
        val audio = LocalAudio(
            mediaStoreId = 1000000069L,
            title = "【官方MV】Mojito - 周杰伦 - 大家的音乐机",
            artist = "未知艺术家",
            album = "Biu",
            durationMs = 188_000L,
            contentUri = "content://media/external/audio/media/1000000069",
        )
        val task = AudioDownloadTaskEntity(
            taskId = "BV1-test:123",
            bvid = "BV1-test",
            cid = 123L,
            title = "【官方MV】Mojito - 周杰伦",
            artist = "大家的音乐机",
            artworkUrl = "https://example.invalid/mojito.jpg",
            qualityPreference = "HIGHEST",
            displayName = "【官方MV】Mojito - 周杰伦 - 大家的音乐机.m4a",
            status = AudioDownloadStatus.COMPLETED.name,
            downloadedBytes = 4_808_168L,
            totalBytes = 4_808_168L,
            tempFilePath = "/tmp/mojito.part",
            qualityLabel = "204 kbps",
            publishedUri = "content://media/external_primary/audio/media/1000000069",
            errorMessage = null,
            createdAtEpochMs = 1L,
            updatedAtEpochMs = 2L,
        )

        val restored = LocalAudioDownloadMetadataPolicy.apply(listOf(audio), listOf(task)).single()

        assertEquals("【官方MV】Mojito - 周杰伦", restored.title)
        assertEquals("大家的音乐机", restored.artist)
        assertEquals("https://example.invalid/mojito.jpg", restored.artworkUri)
    }
}

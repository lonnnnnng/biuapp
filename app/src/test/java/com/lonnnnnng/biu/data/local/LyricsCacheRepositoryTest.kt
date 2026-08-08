package com.lonnnnnng.biu.data.local

import com.lonnnnnng.biu.data.lyrics.LyricsDocument
import com.lonnnnnng.biu.data.lyrics.LyricsLine
import com.lonnnnnng.biu.data.lyrics.LyricsSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LyricsCacheRepositoryTest {
    @Test
    fun `歌词偏移与双语时间轴随缓存一起恢复`() = runBlocking {
        val dao = InMemoryLyricsCacheDao()
        val repository = LyricsCacheRepository(dao, nowEpochMs = { 123L })
        val rawLyrics = "[00:01.00]Hello\n[00:01.00]你好"

        repository.save(
            LyricsDocument(
                cacheKey = "BV1:100",
                source = LyricsSource.LRCLIB,
                lines = listOf(LyricsLine(1_000L, "Hello", "你好")),
                rawLyrics = rawLyrics,
                providerId = 9L,
                trackName = "测试歌曲",
                artistName = "测试歌手",
                isUserSelected = true,
                offsetMs = 1_500L,
            ),
        )

        val restored = repository.find("BV1:100")
        assertNotNull(restored)
        assertEquals(1_500L, restored?.offsetMs)
        assertEquals(listOf(LyricsLine(1_000L, "Hello", "你好")), restored?.lines)
        assertEquals(123L, dao.entity?.updatedAtEpochMs)
    }

    private class InMemoryLyricsCacheDao : LyricsCacheDao {
        var entity: LyricsCacheEntity? = null

        override suspend fun find(cacheKey: String): LyricsCacheEntity? {
            return entity?.takeIf { it.cacheKey == cacheKey }
        }

        override suspend fun upsert(entity: LyricsCacheEntity) {
            this.entity = entity
        }

        override suspend fun delete(cacheKey: String) {
            if (entity?.cacheKey == cacheKey) entity = null
        }
    }
}

package com.lonnnnnng.biu.ui

import com.lonnnnnng.biu.data.lyrics.LyricsDocument
import com.lonnnnnng.biu.data.lyrics.LyricsLine
import com.lonnnnnng.biu.data.lyrics.LyricsSource
import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsEntryPolicyTest {
    private val document = LyricsDocument(
        cacheKey = "BV1:1",
        source = LyricsSource.LRCLIB,
        lines = listOf(LyricsLine(0L, "歌词")),
        rawLyrics = "[00:00]歌词",
    )

    @Test
    fun `当前曲目缓存命中时直接显示歌词`() {
        assertEquals(
            LyricsEntryDestination.SHOW_LYRICS,
            LyricsEntryPolicy.destination(
                expectedCacheKey = "BV1:1",
                state = LyricsUiState(
                    cacheKey = "BV1:1",
                    status = LyricsLoadStatus.LOADED,
                    document = document,
                ),
            ),
        )
    }

    @Test
    fun `缓存缺失或读取失败时进入手动搜索`() {
        listOf(LyricsLoadStatus.IDLE, LyricsLoadStatus.EMPTY, LyricsLoadStatus.ERROR).forEach { status ->
            assertEquals(
                LyricsEntryDestination.SHOW_SEARCH,
                LyricsEntryPolicy.destination(
                    expectedCacheKey = "BV1:1",
                    state = LyricsUiState(cacheKey = "BV1:1", status = status),
                ),
            )
        }
    }

    @Test
    fun `旧曲目结果和缓存读取中保持等待`() {
        assertEquals(
            LyricsEntryDestination.WAIT_FOR_CACHE,
            LyricsEntryPolicy.destination(
                expectedCacheKey = "BV1:1",
                state = LyricsUiState(cacheKey = "BV2:2", status = LyricsLoadStatus.LOADED, document = document),
            ),
        )
        assertEquals(
            LyricsEntryDestination.WAIT_FOR_CACHE,
            LyricsEntryPolicy.destination(
                expectedCacheKey = "BV1:1",
                state = LyricsUiState(cacheKey = "BV1:1", status = LyricsLoadStatus.LOADING),
            ),
        )
    }
}

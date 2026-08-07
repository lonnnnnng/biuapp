package com.lonnnnnng.biu.ui

import com.lonnnnnng.biu.data.bilibili.AccountLibrarySection
import com.lonnnnnng.biu.data.bilibili.BilibiliLibraryVideo
import com.lonnnnnng.biu.data.bilibili.BilibiliVideo
import com.lonnnnnng.biu.data.local.PlaybackHistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedHistoryPolicyTest {
    @Test
    fun `旧版拆分历史入口统一迁移到历史页`() {
        assertEquals(AccountLibrarySection.HISTORY, AccountLibrarySection.ONLINE_HISTORY.normalizedLibrarySection())
        assertEquals(AccountLibrarySection.HISTORY, AccountLibrarySection.LOCAL_HISTORY.normalizedLibrarySection())
        assertEquals(AccountLibrarySection.FAVORITES, AccountLibrarySection.FAVORITES.normalizedLibrarySection())
    }

    @Test
    fun `合并历史按播放时间倒序并保留来源`() {
        val online = online("BVonline", "在线歌曲", savedAt = 300L)
        val local = local("local:1", "本地歌曲", playedAt = 400_000L)

        val result = UnifiedHistoryPolicy.merge(
            online = listOf(online),
            local = listOf(local),
            source = HistorySourceFilter.ALL,
            query = "",
        )

        assertEquals(listOf("local:local:1", "online-key"), result.map(UnifiedHistoryItem::key))
        assertTrue(result[0] is UnifiedHistoryItem.Local)
        assertTrue(result[1] is UnifiedHistoryItem.Online)
    }

    @Test
    fun `来源筛选只返回对应历史`() {
        val result = UnifiedHistoryPolicy.merge(
            online = listOf(online("BVonline", "在线歌曲", savedAt = 300L)),
            local = listOf(local("local:1", "本地歌曲", playedAt = 400_000L)),
            source = HistorySourceFilter.ONLINE,
            query = "",
        )

        assertEquals(1, result.size)
        assertTrue(result.single() is UnifiedHistoryItem.Online)
    }

    @Test
    fun `标题或作者关键词同时过滤两种来源`() {
        val result = UnifiedHistoryPolicy.merge(
            online = listOf(online("BVonline", "在线歌曲", savedAt = 300L, author = "甲")),
            local = listOf(local("local:1", "本地歌曲", playedAt = 400_000L, artist = "乙")),
            source = HistorySourceFilter.ALL,
            query = "乙",
        )

        assertEquals(listOf("local:local:1"), result.map(UnifiedHistoryItem::key))
    }

    @Test
    fun `在线分页重复条目按历史键去重`() {
        val first = online("BVsame", "同一首", savedAt = 300L)
        val duplicate = online("BVsame", "同一首", savedAt = 200L)

        val result = UnifiedHistoryPolicy.merge(
            online = listOf(first, duplicate),
            local = emptyList(),
            source = HistorySourceFilter.ONLINE,
            query = "",
        )

        assertEquals(1, result.size)
        assertEquals("online-key", result.single().key)
    }

    private fun online(
        bvid: String,
        title: String,
        savedAt: Long,
        author: String = "UP 主",
    ) = BilibiliLibraryVideo(
        video = BilibiliVideo(
            bvid = bvid,
            aid = null,
            title = title,
            author = author,
            coverUrl = "https://example.com/$bvid.jpg",
            durationSeconds = 180,
            playCount = 1,
        ),
        savedAtEpochSeconds = savedAt,
        historyKey = "online-key",
    )

    private fun local(
        mediaId: String,
        title: String,
        playedAt: Long,
        artist: String = "本地作者",
    ) = PlaybackHistoryEntity(
        mediaId = mediaId,
        bvid = "BVlocal",
        cid = 1L,
        title = title,
        artist = artist,
        artworkUrl = null,
        qualityPreference = "HIGHEST",
        lastPositionMs = 0L,
        durationMs = 180_000L,
        playedAtEpochMs = playedAt,
        playCount = 1,
    )
}

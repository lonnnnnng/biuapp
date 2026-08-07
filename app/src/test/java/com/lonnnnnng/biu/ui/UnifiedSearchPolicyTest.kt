package com.lonnnnnng.biu.ui

import com.lonnnnnng.biu.data.bilibili.BilibiliCreatorCollection
import com.lonnnnnng.biu.data.bilibili.BilibiliCreatorCollectionType
import com.lonnnnnng.biu.data.local.LocalPlaylistEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class UnifiedSearchPolicyTest {
    @Test
    fun `合集可按名称或UP主筛选并按发布时间倒序`() {
        val result = UnifiedSearchPolicy.filterCollections(
            collections = listOf(
                collection(1L, "现场合集", "甲", 100L),
                collection(2L, "翻唱", "目标UP", 300L),
                collection(3L, "不相关", "乙", 400L),
            ),
            keyword = "目标",
        )

        assertEquals(listOf(2L), result.map(BilibiliCreatorCollection::id))
    }

    @Test
    fun `本地歌单按名称筛选并优先最近更新`() {
        val result = UnifiedSearchPolicy.filterPlaylists(
            playlists = listOf(playlist(1L, "通勤", 100L), playlist(2L, "通勤精选", 300L), playlist(3L, "现场", 400L)),
            keyword = "通勤",
        )

        assertEquals(listOf(2L, 1L), result.map(LocalPlaylistEntity::playlistId))
    }

    private fun collection(id: Long, title: String, owner: String, publishedAt: Long) = BilibiliCreatorCollection(
        id = id,
        type = BilibiliCreatorCollectionType.SEASON,
        title = title,
        coverUrl = "",
        mediaCount = 1,
        ownerMid = id,
        ownerName = owner,
        publishedAtEpochSeconds = publishedAt,
    )

    private fun playlist(id: Long, name: String, updatedAt: Long) = LocalPlaylistEntity(
        playlistId = id,
        name = name,
        position = 0,
        createdAtEpochMs = 1L,
        updatedAtEpochMs = updatedAt,
    )
}

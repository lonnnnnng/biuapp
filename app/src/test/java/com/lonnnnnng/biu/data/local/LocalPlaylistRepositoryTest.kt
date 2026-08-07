package com.lonnnnnng.biu.data.local

import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import com.lonnnnnng.biu.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalPlaylistRepositoryTest {
    @Test
    fun `歌单用bvid和cid区分同一视频的不同分P`() = runBlocking {
        val dao = FakeLocalPlaylistDao()
        val repository = LocalPlaylistRepository(dao, nowEpochMs = { 1000L })
        val playlistId = repository.create("常听", 0)

        repository.addTrack(playlistId, biliTrack(cid = 11L, page = "P1 · 第一首"))
        repository.addTrack(playlistId, biliTrack(cid = 12L, page = "P2 · 第二首"))

        val items = repository.items(playlistId).first()
        assertEquals(listOf("BV1TEST:11", "BV1TEST:12"), items.map { it.mediaId })
        assertEquals(listOf(11L, 12L), items.map { it.cid })
        assertEquals(listOf(0, 1), items.map { it.position })
        assertNull(items.first().streamUrl)
    }

    private fun biliTrack(cid: Long, page: String) = Track(
        id = "BV1TEST:$cid",
        title = "测试合集 · ${page.substringAfter(" · ")}",
        artist = "UP",
        streamUrl = "https://expired.example/$cid.m4s",
        pageTitle = page,
        source = BilibiliTrackSource("BV1TEST", cid),
    )

    private class FakeLocalPlaylistDao : LocalPlaylistDao {
        private val playlistsFlow = MutableStateFlow<List<LocalPlaylistEntity>>(emptyList())
        private val itemFlows = mutableMapOf<Long, MutableStateFlow<List<LocalPlaylistItemEntity>>>()
        private var nextId = 1L

        override fun observePlaylists(): Flow<List<LocalPlaylistEntity>> = playlistsFlow

        override fun observeItems(playlistId: Long): Flow<List<LocalPlaylistItemEntity>> =
            itemFlows.getOrPut(playlistId) { MutableStateFlow(emptyList()) }

        override suspend fun insertPlaylist(playlist: LocalPlaylistEntity): Long {
            val id = nextId++
            playlistsFlow.value = playlistsFlow.value + playlist.copy(playlistId = id)
            return id
        }

        override suspend fun upsertPlaylist(playlist: LocalPlaylistEntity) {
            playlistsFlow.value = playlistsFlow.value.filterNot { it.playlistId == playlist.playlistId } + playlist
        }

        override suspend fun deletePlaylist(playlistId: Long) {
            playlistsFlow.value = playlistsFlow.value.filterNot { it.playlistId == playlistId }
            itemFlows.remove(playlistId)
        }

        override suspend fun nextItemPosition(playlistId: Long): Int =
            itemFlows[playlistId]?.value?.maxOfOrNull { it.position }?.plus(1) ?: 0

        override suspend fun upsertItem(item: LocalPlaylistItemEntity) {
            val flow = itemFlows.getOrPut(item.playlistId) { MutableStateFlow(emptyList()) }
            flow.value = (flow.value.filterNot { it.mediaId == item.mediaId } + item).sortedBy { it.position }
        }

        override suspend fun deleteItem(playlistId: Long, mediaId: String) {
            val flow = itemFlows.getOrPut(playlistId) { MutableStateFlow(emptyList()) }
            flow.value = flow.value.filterNot { it.mediaId == mediaId }
        }

        override suspend fun updateItemPosition(playlistId: Long, mediaId: String, position: Int) {
            val flow = itemFlows.getOrPut(playlistId) { MutableStateFlow(emptyList()) }
            flow.value = flow.value.map { item ->
                if (item.mediaId == mediaId) item.copy(position = position) else item
            }.sortedBy { it.position }
        }
    }
}

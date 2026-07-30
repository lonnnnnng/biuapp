package com.lonnnnnng.biu.data.local

import com.lonnnnnng.biu.core.model.AudioQualityPreference
import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import com.lonnnnnng.biu.core.model.Track
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackQueueRepositoryTest {
    @Test
    fun `在线分P和本地内容往返后保留队列顺序与恢复进度`() = runBlocking {
        val dao = FakePlaybackQueueDao()
        val repository = PlaybackQueueRepository(dao, nowEpochMs = { 8_800L })
        val queue = PlaybackQueueRecord(
            items = listOf(
                Track(
                    id = "BVQUEUE:101",
                    title = "主标题 · 第一首",
                    artist = "测试作者",
                    streamUrl = "https://example.com/101.m4s",
                    artworkUrl = "https://example.com/cover.jpg",
                    qualityLabel = "192 kbps",
                    pageTitle = "P1 · 第一首",
                    source = BilibiliTrackSource(
                        bvid = "BVQUEUE",
                        cid = 101L,
                        qualityPreference = AudioQualityPreference.DATA_SAVER,
                    ),
                ),
                Track(
                    id = "local:22",
                    title = "本地歌曲",
                    artist = "本地作者",
                    streamUrl = "content://media/external/audio/media/22",
                ),
            ),
            currentIndex = 1,
            currentPositionMs = 12_345L,
        )

        repository.replace(queue)

        assertEquals(queue, repository.load())
        assertEquals(8_800L, dao.state?.updatedAtEpochMs)
    }

    @Test
    fun `损坏的索引和负进度恢复到安全范围`() = runBlocking {
        val dao = FakePlaybackQueueDao().apply {
            state = PlaybackQueueStateEntity(currentIndex = 99, currentPositionMs = -50L, updatedAtEpochMs = 1L)
            items += PlaybackQueueItemEntity(
                position = 0,
                mediaId = "local:1",
                title = "测试音频",
                artist = "作者",
                streamUrl = "content://media/external/audio/media/1",
                artworkUrl = null,
                qualityLabel = null,
                pageTitle = null,
                bvid = null,
                cid = null,
                qualityPreference = null,
            )
        }

        val restored = PlaybackQueueRepository(dao).load()

        assertEquals(0, restored?.currentIndex)
        assertEquals(0L, restored?.currentPositionMs)
    }

    @Test
    fun `清空队列同时删除状态和所有媒体项`() = runBlocking {
        val dao = FakePlaybackQueueDao()
        val repository = PlaybackQueueRepository(dao)
        repository.replace(
            PlaybackQueueRecord(
                items = listOf(
                    Track(
                        id = "local:1",
                        title = "测试音频",
                        artist = "作者",
                        streamUrl = "content://media/external/audio/media/1",
                    ),
                ),
                currentIndex = 0,
                currentPositionMs = 0L,
            ),
        )

        repository.clear()

        assertNull(repository.load())
        assertEquals(emptyList<PlaybackQueueItemEntity>(), dao.items)
    }

    private class FakePlaybackQueueDao : PlaybackQueueDao {
        var state: PlaybackQueueStateEntity? = null
        val items = mutableListOf<PlaybackQueueItemEntity>()

        override suspend fun currentState(): PlaybackQueueStateEntity? = state

        override suspend fun currentItems(): List<PlaybackQueueItemEntity> = items.sortedBy { it.position }

        override suspend fun upsertState(state: PlaybackQueueStateEntity) {
            this.state = state
        }

        override suspend fun insertItems(items: List<PlaybackQueueItemEntity>) {
            this.items += items
        }

        override suspend fun deleteState() {
            state = null
        }

        override suspend fun deleteItems() {
            items.clear()
        }
    }
}

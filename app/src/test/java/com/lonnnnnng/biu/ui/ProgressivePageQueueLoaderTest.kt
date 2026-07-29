package com.lonnnnnng.biu.ui

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressivePageQueueLoaderTest {
    @Test
    fun `选中页最先发布且补齐后保持原分P顺序`() = runBlocking {
        val publishedPages = mutableListOf<Int>()
        val queue = mutableListOf<Int>()
        val loader = ProgressivePageQueueLoader(resolve = { pageIndex -> pageIndex })

        loader.load(
            pageCount = 5,
            startIndex = 2,
            onSelected = { pageIndex ->
                publishedPages += pageIndex
                queue += pageIndex
            },
            onExpansion = { expansion ->
                publishedPages += expansion.item
                when (expansion.placement) {
                    QueuePlacement.PREPEND -> queue.add(0, expansion.item)
                    QueuePlacement.APPEND -> queue.add(expansion.item)
                }
            },
        )

        assertEquals(2, publishedPages.first())
        assertEquals(listOf(0, 1, 2, 3, 4), queue)
    }

    @Test
    fun `队列快照保留补齐顺序和当前页进度`() {
        val store = PlaybackQueueSnapshotStore<Int>(Int::toString)

        store.replace(queueId = 7L, items = listOf(2), startIndex = 0, startPositionMs = 0L)
        store.expand(queueId = 7L, placement = QueuePlacement.PREPEND, item = 1)
        store.expand(queueId = 7L, placement = QueuePlacement.PREPEND, item = 0)
        store.expand(queueId = 7L, placement = QueuePlacement.APPEND, item = 3)
        store.updateResumePosition(mediaId = "3", positionMs = 12_345L)

        assertEquals(
            PlaybackQueueSnapshot(
                queueId = 7L,
                items = listOf(0, 1, 2, 3),
                startIndex = 3,
                startPositionMs = 12_345L,
            ),
            store.current(),
        )
    }

    @Test
    fun `旧代数和重复分P不会污染当前快照`() {
        val store = PlaybackQueueSnapshotStore<Int>(Int::toString)
        store.replace(queueId = 8L, items = listOf(2), startIndex = 0, startPositionMs = 0L)

        assertEquals(null, store.expand(queueId = 7L, placement = QueuePlacement.APPEND, item = 3))
        store.expand(queueId = 8L, placement = QueuePlacement.APPEND, item = 3)
        store.expand(queueId = 8L, placement = QueuePlacement.APPEND, item = 3)

        assertEquals(listOf(2, 3), store.current()?.items)
    }
}

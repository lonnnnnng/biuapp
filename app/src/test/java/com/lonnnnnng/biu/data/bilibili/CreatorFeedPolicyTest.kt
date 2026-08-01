package com.lonnnnnng.biu.data.bilibili

import org.junit.Assert.assertEquals
import org.junit.Test

class CreatorFeedPolicyTest {
    @Test
    fun `配置增删只清理失效UP并保留未变Tab缓存`() {
        val retainedCreator = BilibiliCreator(mid = 1001L, name = "保留UP", faceUrl = "")
        val removedCreator = BilibiliCreator(mid = 1002L, name = "移除UP", faceUrl = "")
        val addedCreator = BilibiliCreator(mid = 1003L, name = "新增UP", faceUrl = "")
        val existing = listOf(
            CreatorFeedTabState(
                creator = retainedCreator,
                videos = listOf(video("BV-KEEP", 300L)),
                nextPage = 3,
                hasLoaded = true,
            ),
            CreatorFeedTabState(creator = removedCreator, hasLoaded = true),
        )

        val tabs = CreatorFeedPolicy.reconcileTabs(existing, listOf(retainedCreator, addedCreator))

        assertEquals(listOf(1001L, 1003L), tabs.map { it.creator.mid })
        assertEquals(listOf("BV-KEEP"), tabs.first().videos.map(BilibiliVideo::bvid))
        assertEquals(3, tabs.first().nextPage)
        assertEquals(1, tabs.last().nextPage)
        assertEquals(false, tabs.last().hasLoaded)
    }

    @Test
    fun `续页只推进当前UP并按BV号去重`() {
        val creator = BilibiliCreator(mid = 1001L, name = "测试UP", faceUrl = "")
        val current = CreatorFeedTabState(
            creator = creator,
            videos = listOf(video("BV-OLD", 100L), video("BV-SHARED", 200L)),
            nextPage = 2,
            hasLoaded = true,
            isLoadingMore = true,
        )
        val page = BilibiliCreatorVideoPage(
            videos = listOf(video("BV-SHARED", 200L), video("BV-NEW", 300L)),
            page = 2,
            hasMore = false,
        )

        val updated = CreatorFeedPolicy.applyPage(current, page, append = true)

        assertEquals(listOf("BV-OLD", "BV-SHARED", "BV-NEW"), updated.videos.map(BilibiliVideo::bvid))
        assertEquals(null, updated.nextPage)
        assertEquals(false, updated.isLoadingMore)
    }

    private fun video(bvid: String, publishedAt: Long?) = BilibiliVideo(
        bvid = bvid,
        aid = null,
        title = bvid,
        author = "UP",
        coverUrl = "",
        durationSeconds = 60,
        playCount = null,
        publishedAtEpochSeconds = publishedAt,
    )
}

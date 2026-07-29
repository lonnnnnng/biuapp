package com.lonnnnnng.biu.data.bilibili

import org.junit.Assert.assertEquals
import org.junit.Test

class CreatorFeedPolicyTest {
    @Test
    fun `空配置使用音乐区和音乐榜作为首页来源`() {
        assertEquals(HomeFeedMode.FALLBACK, CreatorFeedPolicy.modeFor(emptyList()))
    }

    @Test
    fun `选中任意关注UP后首页切换为我的关注`() {
        val selected = listOf(BilibiliCreator(mid = 1001L, name = "测试UP", faceUrl = ""))

        assertEquals(HomeFeedMode.MY_FOLLOWS, CreatorFeedPolicy.modeFor(selected))
    }

    @Test
    fun `多个UP投稿按发布时间倒排并按BV号去重`() {
        val firstCreator = listOf(
            video("BV-OLD", 100L),
            video("BV-SHARED", 200L),
        )
        val secondCreator = listOf(
            video("BV-NEW", 300L),
            video("BV-SHARED", 150L),
            video("BV-UNKNOWN", null),
        )

        val merged = CreatorFeedPolicy.merge(listOf(firstCreator, secondCreator))

        assertEquals(listOf("BV-NEW", "BV-SHARED", "BV-OLD", "BV-UNKNOWN"), merged.map(BilibiliVideo::bvid))
        assertEquals(200L, merged.first { it.bvid == "BV-SHARED" }.publishedAtEpochSeconds)
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

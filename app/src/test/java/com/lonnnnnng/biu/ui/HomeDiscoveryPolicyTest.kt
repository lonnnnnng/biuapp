package com.lonnnnnng.biu.ui

import com.lonnnnnng.biu.data.bilibili.BilibiliCreator
import com.lonnnnnng.biu.data.bilibili.BilibiliVideo
import com.lonnnnnng.biu.data.bilibili.CreatorFeedTabState
import com.lonnnnnng.biu.data.local.CreatorGroupEntity
import com.lonnnnnng.biu.data.local.PlaybackHistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeDiscoveryPolicyTest {
    @Test
    fun `来源选项包含全部有效分组和每位UP`() {
        val creators = listOf(creator(1L, "甲"), creator(2L, "乙"))
        val groups = listOf(group(10L, "翻唱"), group(20L, "空分组"))

        val options = HomeDiscoveryPolicy.scopeOptions(
            creators = creators,
            groups = groups,
            memberships = mapOf(10L to setOf(2L), 20L to setOf(99L)),
        )

        assertEquals(listOf("全部", "翻唱", "甲", "乙"), options.map(HomeDiscoveryScopeOption::label))
    }

    @Test
    fun `三种发现模式分别按发布时间和播放历史筛选`() {
        val creators = listOf(creator(1L, "甲"), creator(2L, "乙"))
        val tabs = listOf(
            tab(creators[0], video("BV-old", 100L), video("BV-new", 400L)),
            tab(creators[1], video("BV-mid", 300L)),
        )
        val history = listOf(history("BV-old", 900L), history("BV-mid", 700L))

        val latest = snapshot(tabs, creators, history, HomeDiscoveryMode.LATEST)
        val unplayed = snapshot(tabs, creators, history, HomeDiscoveryMode.UNPLAYED)
        val recent = snapshot(tabs, creators, history, HomeDiscoveryMode.RECENT)

        assertEquals(listOf("BV-new", "BV-mid", "BV-old"), latest.videos.map(BilibiliVideo::bvid))
        assertEquals(listOf("BV-new"), unplayed.videos.map(BilibiliVideo::bvid))
        assertEquals(listOf("BV-old", "BV-mid"), recent.videos.map(BilibiliVideo::bvid))
    }

    @Test
    fun `分组范围只聚合分组内已选UP`() {
        val creators = listOf(creator(1L, "甲"), creator(2L, "乙"))
        val tabs = listOf(tab(creators[0], video("BV-a", 100L)), tab(creators[1], video("BV-b", 200L)))

        val result = HomeDiscoveryPolicy.snapshot(
            tabs = tabs,
            scope = HomeDiscoveryScope.Group(10L),
            mode = HomeDiscoveryMode.LATEST,
            creators = creators,
            memberships = mapOf(10L to setOf(2L, 99L)),
            history = emptyList(),
        )

        assertEquals(listOf("BV-b"), result.videos.map(BilibiliVideo::bvid))
    }

    @Test
    fun `聚合续页优先推进发布时间边界较新的UP`() {
        val first = creator(1L, "甲")
        val second = creator(2L, "乙")
        val nextMid = HomeDiscoveryPolicy.nextAppendMid(
            tabs = listOf(
                tab(first, video("BV-a", 200L), nextPage = 2),
                tab(second, video("BV-b", 500L), nextPage = 2),
            ),
            scope = HomeDiscoveryScope.All,
            creators = listOf(first, second),
            memberships = emptyMap(),
        )

        assertEquals(2L, nextMid)
    }

    @Test
    fun `过滤后可见列表不变时来源分页键仍会前进`() {
        val creator = creator(1L, "甲")
        val history = listOf(history("BV-played", 900L))
        val before = snapshot(
            tabs = listOf(tab(creator, video("BV-played", 500L), nextPage = 2)),
            creators = listOf(creator),
            history = history,
            mode = HomeDiscoveryMode.RECENT,
        )
        val after = snapshot(
            tabs = listOf(
                tab(creator, video("BV-played", 500L), video("BV-hidden", 400L), nextPage = 3),
            ),
            creators = listOf(creator),
            history = history,
            mode = HomeDiscoveryMode.RECENT,
        )

        assertEquals(before.videos.map(BilibiliVideo::bvid), after.videos.map(BilibiliVideo::bvid))
        assertEquals(false, before.paginationKey == after.paginationKey)
    }

    private fun snapshot(
        tabs: List<CreatorFeedTabState>,
        creators: List<BilibiliCreator>,
        history: List<PlaybackHistoryEntity>,
        mode: HomeDiscoveryMode,
    ) = HomeDiscoveryPolicy.snapshot(
        tabs = tabs,
        scope = HomeDiscoveryScope.All,
        mode = mode,
        creators = creators,
        memberships = emptyMap(),
        history = history,
    )

    private fun creator(mid: Long, name: String) = BilibiliCreator(mid = mid, name = name, faceUrl = "")

    private fun group(id: Long, name: String) = CreatorGroupEntity(
        groupId = id,
        name = name,
        position = id.toInt(),
        createdAtEpochMs = 1L,
        updatedAtEpochMs = 1L,
    )

    private fun tab(
        creator: BilibiliCreator,
        vararg videos: BilibiliVideo,
        nextPage: Int? = null,
    ) = CreatorFeedTabState(
        creator = creator,
        videos = videos.toList(),
        nextPage = nextPage,
        hasLoaded = true,
    )

    private fun video(bvid: String, publishedAt: Long) = BilibiliVideo(
        bvid = bvid,
        aid = null,
        title = bvid,
        author = "UP",
        coverUrl = "",
        durationSeconds = 60,
        playCount = null,
        publishedAtEpochSeconds = publishedAt,
    )

    private fun history(bvid: String, playedAt: Long) = PlaybackHistoryEntity(
        mediaId = "$bvid:1",
        bvid = bvid,
        cid = 1L,
        title = bvid,
        artist = "UP",
        artworkUrl = null,
        qualityPreference = "HIGHEST",
        lastPositionMs = 0L,
        durationMs = 60_000L,
        playedAtEpochMs = playedAt,
        playCount = 1,
    )
}

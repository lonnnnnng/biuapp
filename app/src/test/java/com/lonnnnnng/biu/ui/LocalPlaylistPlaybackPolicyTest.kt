package com.lonnnnnng.biu.ui

import com.lonnnnnng.biu.core.model.Track
import com.lonnnnnng.biu.data.bilibili.BilibiliApiException
import com.lonnnnnng.biu.data.bilibili.BilibiliVideoDetail
import com.lonnnnnng.biu.data.bilibili.BilibiliVideoPage
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalPlaylistPlaybackPolicyTest {
    @Test
    fun `线上失效但存在本地副本时仍允许播放`() {
        assertEquals(
            true,
            LocalPlaylistPlaybackPolicy.canPlay(
                availability = LocalPlaylistItemAvailability.VIDEO_UNAVAILABLE,
                hasDownloadedCopy = true,
            ),
        )
        assertEquals(
            false,
            LocalPlaylistPlaybackPolicy.canPlay(
                availability = LocalPlaylistItemAvailability.PAGE_UNAVAILABLE,
                hasDownloadedCopy = false,
            ),
        )
    }

    @Test
    fun `临时检查失败不阻断播放入口`() {
        assertEquals(
            true,
            LocalPlaylistPlaybackPolicy.canPlay(
                availability = LocalPlaylistItemAvailability.ERROR,
                hasDownloadedCopy = false,
            ),
        )
    }

    @Test
    fun `歌单开头失效时跳到下一首并保留其余可用曲目顺序`() {
        val second = track("second")
        val third = track("third")

        val plan = LocalPlaylistPlaybackPolicy.plan(
            resolvedTracks = listOf(null, second, third),
            requestedIndex = 0,
        )

        assertEquals(listOf(second, third), plan?.tracks)
        assertEquals(0, plan?.startIndex)
        assertEquals(1, plan?.skippedCount)
    }

    @Test
    fun `点中的曲目失效时优先衔接后续可用曲目`() {
        val first = track("first")
        val third = track("third")

        val plan = LocalPlaylistPlaybackPolicy.plan(
            resolvedTracks = listOf(first, null, third),
            requestedIndex = 1,
        )

        assertEquals(listOf(first, third), plan?.tracks)
        assertEquals(1, plan?.startIndex)
    }

    @Test
    fun `全部失效时不生成播放计划`() {
        assertNull(
            LocalPlaylistPlaybackPolicy.plan(
                resolvedTracks = listOf(null, null),
                requestedIndex = 0,
            ),
        )
    }

    @Test
    fun `B站视频和分P失效会映射为不同状态`() {
        val missingVideo = LocalPlaylistAvailabilityPolicy.fromBilibiliDetail(
            detailResult = Result.failure(BilibiliApiException(-404, "稿件不可用")),
            cid = 11L,
        )
        val missingPage = LocalPlaylistAvailabilityPolicy.fromBilibiliDetail(
            detailResult = Result.success(detail(cid = 12L)),
            cid = 11L,
        )

        assertEquals(LocalPlaylistItemAvailability.VIDEO_UNAVAILABLE, missingVideo)
        assertEquals(LocalPlaylistItemAvailability.PAGE_UNAVAILABLE, missingPage)
    }

    @Test
    fun `临时网络错误和本地文件不可读不会伪装成视频失效`() {
        val networkError = LocalPlaylistAvailabilityPolicy.fromBilibiliDetail(
            detailResult = Result.failure(IOException("offline")),
            cid = 11L,
        )
        val riskControlError = LocalPlaylistAvailabilityPolicy.fromBilibiliDetail(
            detailResult = Result.failure(BilibiliApiException(-412, "请求被拦截")),
            cid = 11L,
        )

        assertEquals(LocalPlaylistItemAvailability.ERROR, networkError)
        assertEquals(LocalPlaylistItemAvailability.ERROR, riskControlError)
        assertEquals(
            LocalPlaylistItemAvailability.LOCAL_FILE_UNAVAILABLE,
            LocalPlaylistAvailabilityPolicy.fromLocalUri(readable = false),
        )
    }

    private fun track(id: String) = Track(
        id = id,
        title = id,
        artist = "UP",
        streamUrl = "https://example.com/$id.m4s",
    )

    private fun detail(cid: Long) = BilibiliVideoDetail(
        bvid = "BV1TEST",
        title = "测试视频",
        author = "UP",
        coverUrl = "",
        pages = listOf(
            BilibiliVideoPage(
                cid = cid,
                page = 1,
                title = "第一首",
                durationSeconds = 180,
                coverUrl = null,
            ),
        ),
    )
}

package com.lonnnnnng.biu.download

import com.lonnnnnng.biu.core.model.AudioQualityPreference
import com.lonnnnnng.biu.data.bilibili.BilibiliVideo
import com.lonnnnnng.biu.data.bilibili.BilibiliVideoDetail
import com.lonnnnnng.biu.data.bilibili.BilibiliVideoPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FavoriteBatchDownloadPolicyTest {
    @Test
    fun `多P收藏资源按页面顺序拆成独立下载任务`() {
        val video = video(title = "收藏资源", author = "列表作者", coverUrl = "https://example.com/list.jpg")
        val detail = BilibiliVideoDetail(
            bvid = video.bvid,
            title = "视频总标题",
            author = "详情作者",
            coverUrl = "https://example.com/detail.jpg",
            pages = listOf(
                BilibiliVideoPage(101L, 1, "第一首", 180, null),
                BilibiliVideoPage(202L, 2, "第二首", 240, "https://example.com/p2.jpg"),
            ),
        )

        val pages = FavoriteBatchDownloadPolicy.expand(video, detail, AudioQualityPreference.DATA_SAVER)

        assertEquals(listOf(101L, 202L), pages.map { it.source.cid })
        assertEquals(listOf("第一首", "第二首"), pages.map(FavoriteDownloadPage::currentTitle))
        assertEquals(listOf("视频总标题", "视频总标题"), pages.map(FavoriteDownloadPage::resourceTitle))
        assertEquals(listOf("https://example.com/detail.jpg", "https://example.com/p2.jpg"), pages.map(FavoriteDownloadPage::artworkUrl))
        assertEquals("BV1BATCH:101", pages.first().toAudioRequest().taskId)
        assertEquals("BV1BATCH:202", pages.last().toVideoRequest().taskId)
        assertEquals(AudioQualityPreference.DATA_SAVER, pages.first().source.qualityPreference)
    }

    @Test
    fun `单P收藏资源使用视频总标题作为文件标题`() {
        val video = video(title = "列表标题", author = "列表作者", coverUrl = "https://example.com/list.jpg")
        val detail = BilibiliVideoDetail(
            bvid = video.bvid,
            title = "单P总标题",
            author = "",
            coverUrl = "",
            pages = listOf(BilibiliVideoPage(303L, 1, "P1", 120, null)),
        )

        val page = FavoriteBatchDownloadPolicy.expand(video, detail, AudioQualityPreference.HIGHEST).single()

        assertEquals("单P总标题", page.currentTitle)
        assertEquals("列表作者", page.artist)
        assertEquals("https://example.com/list.jpg", page.artworkUrl)
    }

    @Test
    fun `没有分P的视频不能创建空批量任务`() {
        val video = video(title = "空资源", author = "作者", coverUrl = "")
        val detail = BilibiliVideoDetail(video.bvid, "空资源", "作者", "", emptyList())

        assertThrows(IllegalArgumentException::class.java) {
            FavoriteBatchDownloadPolicy.expand(video, detail, AudioQualityPreference.HIGHEST)
        }
    }

    private fun video(title: String, author: String, coverUrl: String) = BilibiliVideo(
        bvid = "BV1BATCH",
        aid = 1L,
        title = title,
        author = author,
        coverUrl = coverUrl,
        durationSeconds = null,
        playCount = null,
    )
}

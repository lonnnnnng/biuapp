package com.lonnnnnng.biu.data.bilibili

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BilibiliFavoriteRepositoryTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().also(MockWebServer::start)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `批量下载候选读取收藏夹全部分页且不会被失效条目提前截断`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "has_more": true,
                    "medias": [
                      {"id": 1, "type": 2, "attr": 0, "bvid": "BV1PAGE1", "title": "第一条", "upper": {"name": "UP1"}},
                      {"id": 2, "type": 2, "attr": 1, "bvid": "BV1INVALID", "title": "已失效"}
                    ]
                  }
                }
                """.trimIndent(),
            ),
        )
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "has_more": false,
                    "medias": [
                      {"id": 3, "type": 2, "attr": 0, "bvid": "BV1PAGE2", "title": "第二条", "upper": {"name": "UP2"}}
                    ]
                  }
                }
                """.trimIndent(),
            ),
        )
        val repository = BilibiliRepository(OkHttpClient(), apiBase = server.url("/"))

        val videos = repository.favoriteVideosAll(folderId = 99L)

        assertEquals(listOf("BV1PAGE1", "BV1PAGE2"), videos.map { it.video.bvid })
        val first = server.takeRequest().requestUrl!!
        val second = server.takeRequest().requestUrl!!
        assertEquals("1", first.queryParameter("pn"))
        assertEquals("2", second.queryParameter("pn"))
        assertEquals("20", first.queryParameter("ps"))
    }

    @Test
    fun `我创建的与我收藏的使用独立接口且收藏列表包含视频合集`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "count": 1,
                    "has_more": false,
                    "list": [
                      {"id": 100, "type": 2, "title": "我的歌单", "media_count": 3, "mid": 7}
                    ]
                  }
                }
                """.trimIndent(),
            ),
        )
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "count": 2,
                    "has_more": false,
                    "list": [
                      {"id": 200, "type": 11, "title": "收藏的歌单", "media_count": 5, "mid": 8, "upper": {"name": "歌单作者"}},
                      {"id": 300, "type": 21, "title": "收藏的合集", "media_count": 9, "mid": 9, "upper": {"name": "合集作者"}}
                    ]
                  }
                }
                """.trimIndent(),
            ),
        )
        val repository = BilibiliRepository(OkHttpClient(), apiBase = server.url("/"))

        val created = repository.createdFavoriteFolders(mid = 7L)
        val collected = repository.collectedFavoriteFolders(mid = 7L)

        assertEquals(BilibiliFavoriteFolderGroup.CREATED, created.single().group)
        assertEquals(BilibiliFavoriteFolderType.VIDEO_FOLDER, created.single().type)
        assertEquals(
            listOf(BilibiliFavoriteFolderType.VIDEO_FOLDER, BilibiliFavoriteFolderType.VIDEO_COLLECTION),
            collected.map(BilibiliFavoriteFolder::type),
        )
        assertTrue(collected.all { folder -> folder.group == BilibiliFavoriteFolderGroup.COLLECTED })
        assertEquals(listOf("歌单作者", "合集作者"), collected.map(BilibiliFavoriteFolder::ownerName))

        val createdRequest = server.takeRequest().requestUrl!!
        val collectedRequest = server.takeRequest().requestUrl!!
        assertEquals("/x/v3/fav/folder/created/list", createdRequest.encodedPath)
        assertEquals("/x/v3/fav/folder/collected/list", collectedRequest.encodedPath)
        assertEquals("web", collectedRequest.queryParameter("platform"))
        assertEquals("7", collectedRequest.queryParameter("up_mid"))
    }

    @Test
    fun `视频合集内容使用合集接口而不是普通收藏夹接口`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "wbi_img": {
                      "img_url": "https://i0.hdslb.com/bfs/wbi/abcdefghijklmnopqrstuvwxyz123456.png",
                      "sub_url": "https://i0.hdslb.com/bfs/wbi/123456abcdefghijklmnopqrstuvwxyz.png"
                    }
                  }
                }
                """.trimIndent(),
            ),
        )
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "info": {"media_count": 1},
                    "medias": [
                      {"id": 1, "bvid": "BV1SEASON", "title": "合集歌曲", "cover": "//i0.hdslb.com/season.jpg", "duration": 240, "pubtime": 1700000000, "upper": {"name": "合集作者"}}
                    ]
                  }
                }
                """.trimIndent(),
            ),
        )
        val repository = BilibiliRepository(OkHttpClient(), nowEpochSeconds = { 1700000000L }, apiBase = server.url("/"))
        val folder = BilibiliFavoriteFolder(
            id = 300L,
            title = "收藏的合集",
            coverUrl = "",
            mediaCount = 1,
            type = BilibiliFavoriteFolderType.VIDEO_COLLECTION,
            group = BilibiliFavoriteFolderGroup.COLLECTED,
        )

        val page = repository.favoriteVideoPage(folder)
        val videos = page.videos

        assertEquals("BV1SEASON", videos.single().video.bvid)
        assertEquals("合集作者", videos.single().video.author)
        assertFalse(page.hasMore)
        assertEquals("/x/web-interface/nav", server.takeRequest().requestUrl!!.encodedPath)
        val seasonRequest = server.takeRequest().requestUrl!!
        assertEquals("/x/space/fav/season/list", seasonRequest.encodedPath)
        assertEquals("300", seasonRequest.queryParameter("season_id"))
        assertEquals("1", seasonRequest.queryParameter("pn"))
        assertTrue(seasonRequest.queryParameter("w_rid").orEmpty().isNotBlank())
    }

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}

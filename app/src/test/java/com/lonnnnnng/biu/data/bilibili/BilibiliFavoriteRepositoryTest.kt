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
                    "info": {"media_count": 2},
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
    fun `普通收藏夹详情解析服务端内容数量`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "info": {"media_count": 23},
                    "has_more": true,
                    "medias": [
                      {"id": 1, "type": 2, "attr": 0, "bvid": "BV1COUNT", "title": "计数样本", "upper": {"name": "UP"}}
                    ]
                  }
                }
                """.trimIndent(),
            ),
        )
        val repository = BilibiliRepository(OkHttpClient(), apiBase = server.url("/"))

        val page = repository.favoriteVideoPage(folderId = 99L)

        assertEquals(23, page.mediaCount)
        assertTrue(page.hasMore)
        assertEquals("BV1COUNT", page.videos.single().video.bvid)
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
        assertTrue(created.single().isUserManaged)
        assertEquals(
            listOf(BilibiliFavoriteFolderType.VIDEO_FOLDER, BilibiliFavoriteFolderType.VIDEO_COLLECTION),
            collected.map(BilibiliFavoriteFolder::type),
        )
        assertTrue(collected.all { folder -> folder.group == BilibiliFavoriteFolderGroup.COLLECTED })
        assertTrue(collected.none(BilibiliFavoriteFolder::isUserManaged))
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
        assertEquals(1, page.mediaCount)
        assertFalse(page.hasMore)
        assertEquals("/x/web-interface/nav", server.takeRequest().requestUrl!!.encodedPath)
        val seasonRequest = server.takeRequest().requestUrl!!
        assertEquals("/x/space/fav/season/list", seasonRequest.encodedPath)
        assertEquals("300", seasonRequest.queryParameter("season_id"))
        assertEquals("1", seasonRequest.queryParameter("pn"))
        assertTrue(seasonRequest.queryParameter("w_rid").orEmpty().isNotBlank())
    }

    @Test
    fun `收藏夹新建重命名删除使用CSRF表单`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {"id": 501, "title": "新收藏夹", "cover": "", "media_count": 0, "mid": 7}
                }
                """.trimIndent(),
            ),
        )
        server.enqueue(jsonResponse("{\"code\":0,\"message\":\"0\"}"))
        server.enqueue(jsonResponse("{\"code\":0,\"message\":\"0\"}"))
        val repository = BilibiliRepository(
            client = OkHttpClient(),
            apiBase = server.url("/"),
            csrfProvider = { "csrf-token" },
        )

        val created = repository.createFavoriteFolder(" 新收藏夹 ")
        repository.renameFavoriteFolder(folderId = created.id, title = "改名后")
        repository.deleteFavoriteFolder(created.id)

        assertEquals(501L, created.id)
        assertEquals(BilibiliFavoriteFolderGroup.CREATED, created.group)
        assertEquals(BilibiliFavoriteFolderType.VIDEO_FOLDER, created.type)
        val createRequest = server.takeRequest()
        assertEquals("/x/v3/fav/folder/add", createRequest.requestUrl!!.encodedPath)
        assertEquals("title=%E6%96%B0%E6%94%B6%E8%97%8F%E5%A4%B9&privacy=0&csrf=csrf-token", createRequest.body.readUtf8())
        val renameRequest = server.takeRequest()
        assertEquals("/x/v3/fav/folder/edit", renameRequest.requestUrl!!.encodedPath)
        assertEquals("media_id=501&title=%E6%94%B9%E5%90%8D%E5%90%8E&csrf=csrf-token", renameRequest.body.readUtf8())
        val deleteRequest = server.takeRequest()
        assertEquals("/x/v3/fav/folder/del", deleteRequest.requestUrl!!.encodedPath)
        assertEquals("media_ids=501&csrf=csrf-token", deleteRequest.body.readUtf8())
    }

    @Test
    fun `视频加入和移出收藏夹使用同一deal接口`() = runBlocking {
        server.enqueue(jsonResponse("{\"code\":0,\"message\":\"0\"}"))
        server.enqueue(jsonResponse("{\"code\":0,\"message\":\"0\"}"))
        val repository = BilibiliRepository(
            client = OkHttpClient(),
            apiBase = server.url("/"),
            csrfProvider = { "csrf-token" },
        )

        repository.addVideoToFavorite(aid = 9988L, folderId = 501L)
        repository.removeVideoFromFavorite(aid = 9988L, folderId = 501L)

        val addRequest = server.takeRequest()
        assertEquals("/x/v3/fav/resource/deal", addRequest.requestUrl!!.encodedPath)
        assertEquals(
            "rid=9988&type=2&add_media_ids=501&platform=web&ga=1&gaia_source=web_normal&csrf=csrf-token",
            addRequest.body.readUtf8(),
        )
        val removeRequest = server.takeRequest()
        assertEquals("/x/v3/fav/resource/deal", removeRequest.requestUrl!!.encodedPath)
        assertEquals(
            "rid=9988&type=2&del_media_ids=501&platform=web&ga=1&gaia_source=web_normal&csrf=csrf-token",
            removeRequest.body.readUtf8(),
        )
    }

    @Test
    fun `收藏夹成员关系返回视频是否已存在及权威数量`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "count": 2,
                    "list": [
                      {"id": 501, "title": "已收藏", "media_count": 8, "mid": 7, "fav_state": 1},
                      {"id": 502, "title": "未收藏", "media_count": 3, "mid": 7, "fav_state": 0}
                    ]
                  }
                }
                """.trimIndent(),
            ),
        )
        val repository = BilibiliRepository(OkHttpClient(), apiBase = server.url("/"))

        val memberships = repository.createdFavoriteFolderMemberships(mid = 7L, aid = 9988L)

        assertEquals(listOf(true, false), memberships.map(BilibiliFavoriteFolderMembership::containsVideo))
        assertEquals(listOf(8, 3), memberships.map { membership -> membership.folder.mediaCount })
        val request = server.takeRequest().requestUrl!!
        assertEquals("/x/v3/fav/folder/created/list-all", request.encodedPath)
        assertEquals("7", request.queryParameter("up_mid"))
        assertEquals("2", request.queryParameter("type"))
        assertEquals("9988", request.queryParameter("rid"))
    }

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}

package com.lonnnnnng.biu.data.bilibili

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
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

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}

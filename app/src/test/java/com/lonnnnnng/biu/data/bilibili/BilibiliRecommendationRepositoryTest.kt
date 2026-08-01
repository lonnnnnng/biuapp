package com.lonnnnnng.biu.data.bilibili

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BilibiliRecommendationRepositoryTest {
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
    fun `综合热门使用服务端页码并读取no more`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "no_more": false,
                    "list": [
                      {
                        "aid": 42,
                        "bvid": "BV1POPULAR",
                        "title": "综合热门视频",
                        "pic": "//i0.hdslb.com/popular.jpg",
                        "duration": 195,
                        "pubdate": 1700000100,
                        "owner": {"name": "热门UP"},
                        "stat": {"view": 12345}
                      }
                    ]
                  }
                }
                """.trimIndent(),
            ),
        )
        val repository = BilibiliRepository(OkHttpClient(), apiBase = server.url("/"))

        val result = repository.recommendations(RecommendFeed.COMPREHENSIVE, page = 2)

        assertEquals(2, result.page)
        assertTrue(result.hasMore)
        assertEquals("BV1POPULAR", result.videos.single().bvid)
        assertEquals("热门UP", result.videos.single().author)
        val request = server.takeRequest()
        assertEquals("/x/web-interface/popular", request.requestUrl?.encodedPath)
        assertEquals("2", request.requestUrl?.queryParameter("pn"))
        assertEquals("20", request.requestUrl?.queryParameter("ps"))
    }

    @Test
    fun `每周必看按期数向前翻页`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "list": [
                      {"number": 384, "name": "2026第384期"},
                      {"number": 383, "name": "2026第383期"}
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
                    "list": [
                      {
                        "aid": 43,
                        "bvid": "BV1WEEKLY",
                        "title": "每周必看视频",
                        "pic": "//i0.hdslb.com/weekly.jpg",
                        "duration": 200,
                        "owner": {"name": "每周UP"},
                        "stat": {"view": 23456}
                      }
                    ]
                  }
                }
                """.trimIndent(),
            ),
        )
        val repository = BilibiliRepository(OkHttpClient(), apiBase = server.url("/"))

        val result = repository.recommendations(RecommendFeed.WEEKLY, page = 2)

        assertEquals(listOf("BV1WEEKLY"), result.videos.map(BilibiliVideo::bvid))
        assertFalse(result.hasMore)
        val listRequest = server.takeRequest()
        assertEquals("/x/web-interface/popular/series/list", listRequest.requestUrl?.encodedPath)
        val issueRequest = server.takeRequest()
        assertEquals("/x/web-interface/popular/series/one", issueRequest.requestUrl?.encodedPath)
        assertEquals("383", issueRequest.requestUrl?.queryParameter("number"))
    }

    @Test
    fun `全站排行固定百条数据只在客户端分批展示`() = runBlocking {
        val videos = (1..25).joinToString(",") { index ->
            """{"aid":$index,"bvid":"BV$index","title":"排行$index","pic":"","duration":60,"owner":{"name":"UP$index"},"stat":{"view":$index}}"""
        }
        server.enqueue(jsonResponse("""{"code":0,"data":{"list":[$videos]}}"""))
        val repository = BilibiliRepository(OkHttpClient(), apiBase = server.url("/"))

        val result = repository.recommendations(RecommendFeed.RANKING, page = 2)

        assertEquals((21..25).map { "BV$it" }, result.videos.map(BilibiliVideo::bvid))
        assertFalse(result.hasMore)
        val request = server.takeRequest()
        assertEquals("/x/web-interface/ranking/v2", request.requestUrl?.encodedPath)
        assertEquals("all", request.requestUrl?.queryParameter("type"))
        assertNull(request.requestUrl?.queryParameter("pn"))
        assertNull(request.requestUrl?.queryParameter("rid"))
    }

    private fun jsonResponse(body: String): MockResponse {
        return MockResponse()
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200)
            .setBody(body)
    }
}

package com.lonnnnnng.biu.data.bilibili

import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BilibiliHistoryRepositoryTest {
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
    fun `在线历史使用服务端游标连续加载且保留删除键`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "cursor": {"max": 123, "view_at": 1700000000, "business": "archive", "ps": 20},
                    "list": [
                      {
                        "title": "第一首", "author_name": "歌手", "cover": "//i0.hdslb.com/a.jpg",
                        "duration": 240, "progress": 35, "view_at": 1700000020,
                        "history": {"oid": 9988, "bvid": "BV1FIRST", "business": "archive"}
                      }
                    ]
                  }
                }
                """.trimIndent(),
            ),
        )
        server.enqueue(
            jsonResponse(
                """
                {"code": 0, "data": {"cursor": {"max": 0, "view_at": 0, "business": "", "ps": 20}, "list": []}}
                """.trimIndent(),
            ),
        )
        val repository = BilibiliRepository(OkHttpClient(), apiBase = server.url("/"))

        val first = repository.onlineHistory()
        val second = repository.onlineHistory(first.nextCursor)

        assertEquals("archive_9988", first.videos.single().historyKey)
        assertTrue(first.hasMore)
        assertTrue(second.videos.isEmpty())
        assertFalse(second.hasMore)
        val firstRequest = server.takeRequest().requestUrl!!
        val secondRequest = server.takeRequest().requestUrl!!
        assertEquals("0", firstRequest.queryParameter("max"))
        assertEquals("123", secondRequest.queryParameter("max"))
        assertEquals("1700000000", secondRequest.queryParameter("view_at"))
        assertEquals("archive", secondRequest.queryParameter("business"))
    }

    @Test
    fun `游标页只有失效条目时仍允许继续请求下一页`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "cursor": {"max": 456, "view_at": 1700000100, "business": "archive", "ps": 20},
                    "list": [{"title": "已失效", "history": {"oid": 9, "bvid": "", "business": "archive"}}]
                  }
                }
                """.trimIndent(),
            ),
        )
        val repository = BilibiliRepository(OkHttpClient(), apiBase = server.url("/"))

        val result = repository.onlineHistory()

        assertTrue(result.videos.isEmpty())
        assertTrue(result.hasMore)
        assertEquals(456L, result.nextCursor?.max)
    }

    @Test
    fun `在线历史搜索按标题或UP主关键字分页`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "has_more": true,
                    "page": {"pn": 2, "total": 3},
                    "list": [
                      {
                        "title": "搜索结果", "author_name": "目标UP", "duration": 100, "progress": 9,
                        "history": {"oid": 88, "bvid": "BV1SEARCH", "business": "archive"}
                      }
                    ]
                  }
                }
                """.trimIndent(),
            ),
        )
        val repository = BilibiliRepository(OkHttpClient(), apiBase = server.url("/"))

        val result = repository.searchOnlineHistory(keyword = "目标 UP", page = 2)

        assertEquals("BV1SEARCH", result.videos.single().video.bvid)
        assertEquals(3, result.nextSearchPage)
        assertTrue(result.hasMore)
        val request = server.takeRequest()
        assertEquals("/x/web-interface/history/search", request.requestUrl!!.encodedPath)
        assertEquals("目标 UP", request.requestUrl!!.queryParameter("keyword"))
        assertEquals("archive", request.requestUrl!!.queryParameter("business"))
        assertEquals("2", request.requestUrl!!.queryParameter("pn"))
    }

    @Test
    fun `删除和清空在线历史携带CSRF表单`() = runBlocking {
        server.enqueue(jsonResponse("{\"code\":0,\"message\":\"0\"}"))
        server.enqueue(jsonResponse("{\"code\":0,\"message\":\"0\"}"))
        val repository = BilibiliRepository(
            client = OkHttpClient(),
            apiBase = server.url("/"),
            csrfProvider = { "csrf-token" },
        )

        repository.deleteOnlineHistory("archive_9988")
        repository.clearOnlineHistory()

        val deleteRequest = server.takeRequest()
        assertEquals("/x/v2/history/delete", deleteRequest.requestUrl!!.encodedPath)
        assertEquals("kid=archive_9988&csrf=csrf-token", deleteRequest.body.readUtf8())
        val clearRequest = server.takeRequest()
        assertEquals("/x/v2/history/clear", clearRequest.requestUrl!!.encodedPath)
        assertEquals("csrf=csrf-token", clearRequest.body.readUtf8())
    }

    @Test
    fun `在线历史变更缺少CSRF时不发送请求`() {
        val repository = BilibiliRepository(
            client = OkHttpClient(),
            apiBase = server.url("/"),
            csrfProvider = { null },
        )

        assertThrows(IllegalStateException::class.java) {
            runBlocking { repository.clearOnlineHistory() }
        }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `在线历史登录失效保留API错误码`() {
        server.enqueue(jsonResponse("{\"code\":-101,\"message\":\"账号未登录\"}"))
        val repository = BilibiliRepository(OkHttpClient(), apiBase = server.url("/"))

        val error = assertThrows(BilibiliApiException::class.java) {
            runBlocking { repository.onlineHistory() }
        }

        assertEquals(-101, error.code)
    }

    @Test
    fun `在线历史将HTTP错误作为网络错误`() {
        server.enqueue(MockResponse().setResponseCode(503).setBody("service unavailable"))
        val repository = BilibiliRepository(OkHttpClient(), apiBase = server.url("/"))

        val error = assertThrows(IOException::class.java) {
            runBlocking { repository.onlineHistory() }
        }

        assertEquals("Bilibili HTTP 503", error.message)
    }

    @Test
    fun `在线历史变更接口错误不被吞掉`() {
        server.enqueue(jsonResponse("{\"code\":-111,\"message\":\"csrf 校验失败\"}"))
        val repository = BilibiliRepository(
            client = OkHttpClient(),
            apiBase = server.url("/"),
            csrfProvider = { "invalid-token" },
        )

        val error = assertThrows(BilibiliApiException::class.java) {
            runBlocking { repository.deleteOnlineHistory("archive_9988") }
        }

        assertEquals(-111, error.code)
    }

    @Test
    fun `播放心跳使用WBI签名与CSRF表单`() = runBlocking {
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
        server.enqueue(jsonResponse("{\"code\":0,\"message\":\"0\"}"))
        val repository = BilibiliRepository(
            client = OkHttpClient(),
            nowEpochSeconds = { 1700000000L },
            apiBase = server.url("/"),
            csrfProvider = { "csrf-token" },
        )

        repository.reportPlayHeartbeat(
            aid = 9988L,
            bvid = "BV1REPORT",
            cid = 7788L,
            session = "session-id",
            startedAtEpochSeconds = 1700000000L,
            playedSeconds = 35,
            maxPlayedSeconds = 40,
            durationSeconds = 240,
            playType = 2,
        )

        assertEquals("/x/web-interface/nav", server.takeRequest().requestUrl!!.encodedPath)
        val heartbeatRequest = server.takeRequest()
        assertEquals("/x/click-interface/web/heartbeat", heartbeatRequest.requestUrl!!.encodedPath)
        assertTrue(heartbeatRequest.requestUrl!!.queryParameter("w_rid").orEmpty().isNotBlank())
        val heartbeatBody = heartbeatRequest.body.readUtf8()
        assertTrue(heartbeatBody.contains("played_time=35"))
        assertTrue(heartbeatBody.contains("play_type=2"))
        assertTrue(heartbeatBody.contains("csrf=csrf-token"))
    }

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}

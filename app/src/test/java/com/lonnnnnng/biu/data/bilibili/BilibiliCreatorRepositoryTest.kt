package com.lonnnnnng.biu.data.bilibili

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BilibiliCreatorRepositoryTest {
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
    fun `读取登录账号关注列表并携带分页参数`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "total": 2,
                    "list": [
                      {"mid": 1001, "uname": "UP一", "face": "//i0.hdslb.com/1.jpg"},
                      {"mid": 1002, "uname": "UP二", "face": "//i0.hdslb.com/2.jpg"}
                    ]
                  }
                }
                """.trimIndent(),
            ),
        )
        val repository = BilibiliRepository(OkHttpClient(), apiBase = server.url("/"))

        val creators = repository.followingCreators(mid = 99L)

        assertEquals(listOf(1001L, 1002L), creators.map(BilibiliCreator::mid))
        val request = server.takeRequest()
        assertEquals("/x/relation/followings", request.requestUrl?.encodedPath)
        assertEquals("99", request.requestUrl?.queryParameter("vmid"))
        assertEquals("1", request.requestUrl?.queryParameter("pn"))
        assertEquals("50", request.requestUrl?.queryParameter("ps"))
    }

    @Test
    fun `关注列表超过二十页时继续读取到接口总数`() = runBlocking {
        repeat(21) { index ->
            server.enqueue(
                jsonResponse(
                    """
                    {
                      "code": 0,
                      "data": {
                        "total": 21,
                        "list": [
                          {"mid": ${index + 1}, "uname": "UP${index + 1}", "face": ""}
                        ]
                      }
                    }
                    """.trimIndent(),
                ),
            )
        }
        val repository = BilibiliRepository(OkHttpClient(), apiBase = server.url("/"))

        val creators = repository.followingCreators(mid = 99L)

        assertEquals(21, creators.size)
        repeat(20) { server.takeRequest() }
        assertEquals("21", server.takeRequest().requestUrl?.queryParameter("pn"))
    }

    @Test
    fun `按发布时间读取UP投稿并使用WBI签名`() = runBlocking {
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
                    "list": {
                      "vlist": [
                        {
                          "aid": 42,
                          "bvid": "BV1CREATOR",
                          "title": "最新投稿",
                          "pic": "//i0.hdslb.com/video.jpg",
                          "length": "03:15",
                          "play": 1234,
                          "created": 1700003000
                        }
                      ]
                    }
                  }
                }
                """.trimIndent(),
            ),
        )
        val repository = BilibiliRepository(
            client = OkHttpClient(),
            nowEpochSeconds = { 1_700_000_000L },
            apiBase = server.url("/"),
        )
        val creator = BilibiliCreator(1001L, "关注的UP", "")

        val videos = repository.creatorVideos(creator)

        assertEquals(listOf("BV1CREATOR"), videos.map(BilibiliVideo::bvid))
        server.takeRequest() // WBI key
        val request = server.takeRequest()
        assertEquals("/x/space/wbi/arc/search", request.requestUrl?.encodedPath)
        assertEquals("1001", request.requestUrl?.queryParameter("mid"))
        assertEquals("pubdate", request.requestUrl?.queryParameter("order"))
        assertEquals("30", request.requestUrl?.queryParameter("ps"))
        assertEquals("1700000000", request.requestUrl?.queryParameter("wts"))
        check(!request.requestUrl?.queryParameter("w_rid").isNullOrBlank())
    }

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}

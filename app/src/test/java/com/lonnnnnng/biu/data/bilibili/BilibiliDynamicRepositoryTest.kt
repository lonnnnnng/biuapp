package com.lonnnnnng.biu.data.bilibili

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BilibiliDynamicRepositoryTest {
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
    fun `动态列表使用服务端游标分页并映射可播放视频`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "has_more": true,
                    "offset": "next-1",
                    "items": [
                      {
                        "id_str": "dynamic-1",
                        "visible": true,
                        "modules": {
                          "module_author": {
                            "mid": 1001,
                            "name": "音乐UP",
                            "face": "//i0.hdslb.com/face.jpg",
                            "pub_ts": 1700000100
                          },
                          "module_dynamic": {
                            "desc": {"text": "新投稿说明"},
                            "major": {
                              "archive": {
                                "aid": "42",
                                "bvid": "BV1DYNAMIC",
                                "title": "动态视频",
                                "cover": "//i0.hdslb.com/cover.jpg",
                                "duration_text": "03:15",
                                "stat": {"play": "1.2万"}
                              }
                            }
                          },
                          "module_stat": {
                            "like": {"count": 9, "forbidden": false, "status": true}
                          }
                        }
                      },
                      {
                        "id_str": "dynamic-invalid",
                        "visible": true,
                        "modules": {"module_dynamic": {"major": {"none": {"tips": "已失效"}}}}
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
                {"code":0,"data":{"has_more":false,"offset":"","items":[]}}
                """.trimIndent(),
            ),
        )
        val repository = BilibiliRepository(OkHttpClient(), apiBase = server.url("/"))

        val firstPage = repository.dynamicFeed()
        val secondPage = repository.dynamicFeed(firstPage.nextOffset)

        assertEquals(1, firstPage.items.size)
        assertEquals("dynamic-1", firstPage.items.single().id)
        assertEquals("BV1DYNAMIC", firstPage.items.single().video.bvid)
        assertEquals("音乐UP", firstPage.items.single().video.author)
        assertEquals(195, firstPage.items.single().video.durationSeconds)
        assertEquals("新投稿说明", firstPage.items.single().description)
        assertEquals(9L, firstPage.items.single().likeCount)
        assertTrue(firstPage.items.single().isLiked)
        assertEquals("next-1", firstPage.nextOffset)
        assertTrue(firstPage.hasMore)
        assertFalse(secondPage.hasMore)

        val firstRequest = server.takeRequest()
        assertEquals("/x/polymer/web-dynamic/v1/feed/all", firstRequest.requestUrl?.encodedPath)
        assertEquals("video", firstRequest.requestUrl?.queryParameter("type"))
        assertEquals("web", firstRequest.requestUrl?.queryParameter("platform"))
        assertEquals("333.1365", firstRequest.requestUrl?.queryParameter("web_location"))
        assertEquals(null, firstRequest.requestUrl?.queryParameter("offset"))
        val secondRequest = server.takeRequest()
        assertEquals("next-1", secondRequest.requestUrl?.queryParameter("offset"))
    }

    @Test
    fun `动态点赞通过JSON正文提交并携带CSRF查询参数`() = runBlocking {
        server.enqueue(jsonResponse("""{"code":0,"data":{}}"""))
        val repository = BilibiliRepository(
            client = OkHttpClient(),
            apiBase = server.url("/"),
            csrfProvider = { "csrf-token" },
        )

        repository.updateDynamicLike(dynamicId = "dynamic-1", liked = true)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/x/dynamic/feed/dyn/thumb", request.requestUrl?.encodedPath)
        assertEquals("csrf-token", request.requestUrl?.queryParameter("csrf"))
        assertTrue(request.getHeader("Content-Type").orEmpty().startsWith("application/json"))
        val body = JSONObject(request.body.readUtf8())
        assertEquals("dynamic-1", body.getString("dyn_id_str"))
        assertEquals(1, body.getInt("up"))
    }

    @Test
    fun `取消点赞使用up二并保留动态ID字符串`() = runBlocking {
        server.enqueue(jsonResponse("""{"code":0,"data":{}}"""))
        val repository = BilibiliRepository(
            client = OkHttpClient(),
            apiBase = server.url("/"),
            csrfProvider = { "csrf-token" },
        )

        repository.updateDynamicLike(dynamicId = "9223372036854775808", liked = false)

        val body = JSONObject(server.takeRequest().body.readUtf8())
        assertEquals("9223372036854775808", body.getString("dyn_id_str"))
        assertEquals(2, body.getInt("up"))
    }

    @Test
    fun `一键三连提交视频BVID并映射服务端状态`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {"code":0,"data":{"like":true,"coin":true,"fav":true,"multiply":2}}
                """.trimIndent(),
            ),
        )
        val repository = BilibiliRepository(
            client = OkHttpClient(),
            apiBase = server.url("/"),
            csrfProvider = { "csrf-token" },
        )

        val result = repository.tripleLike("BV1DYNAMIC")

        assertTrue(result.liked)
        assertTrue(result.coined)
        assertTrue(result.favorited)
        assertEquals(2, result.coinCount)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/x/web-interface/archive/like/triple", request.requestUrl?.encodedPath)
        val form = request.body.readUtf8()
        assertTrue(form.contains("bvid=BV1DYNAMIC"))
        assertTrue(form.contains("csrf=csrf-token"))
    }

    @Test
    fun `互动限流错误保留API错误码供界面区分提示`() = runBlocking {
        server.enqueue(jsonResponse("""{"code":-799,"message":"请求过于频繁"}"""))
        val repository = BilibiliRepository(
            client = OkHttpClient(),
            apiBase = server.url("/"),
            csrfProvider = { "csrf-token" },
        )

        val error = assertThrows(BilibiliApiException::class.java) {
            runBlocking { repository.updateDynamicLike("dynamic-1", liked = true) }
        }

        assertEquals(-799, error.code)
        assertTrue(error.message.orEmpty().contains("请求过于频繁"))
    }

    private fun jsonResponse(body: String): MockResponse {
        return MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(body)
    }
}

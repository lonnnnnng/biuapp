package com.lonnnnnng.biu.data.bilibili

import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import com.lonnnnnng.biu.core.model.mediaText
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BilibiliTrackQueueTest {
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
    fun `多P视频按页面顺序解析为播放队列`() = runBlocking {
        server.enqueue(jsonResponse(videoDetailPayload()))
        server.enqueue(jsonResponse(wbiKeyPayload()))
        server.enqueue(jsonResponse(playUrlPayload("https://cdn.example/101.m4s", 192_000)))
        server.enqueue(jsonResponse(playUrlPayload("https://cdn.example/202.m4s", 128_000)))
        val repository = BilibiliRepository(
            client = OkHttpClient(),
            nowEpochSeconds = { 1_700_000_000L },
            apiBase = server.url("/"),
        )

        val tracks = repository.resolveTracks(video())

        assertEquals(listOf("BVQUEUE:101", "BVQUEUE:202"), tracks.map { it.id })
        assertEquals(listOf("长视频 · 第一首", "长视频 · 第二首"), tracks.map { it.title })
        assertEquals(listOf("P1 · 第一首", "P2 · 第二首"), tracks.map { it.pageTitle })
        assertEquals(listOf(101L, 202L), tracks.map { it.source?.cid })
        val requests = List(4) { server.takeRequest(1, TimeUnit.SECONDS) }
        assertEquals("101", requests[2]?.requestUrl?.queryParameter("cid"))
        assertEquals("202", requests[3]?.requestUrl?.queryParameter("cid"))
    }

    @Test
    fun `历史播放按cid恢复当前分P标题`() = runBlocking {
        server.enqueue(jsonResponse(videoDetailPayload()))
        server.enqueue(jsonResponse(wbiKeyPayload()))
        server.enqueue(jsonResponse(playUrlPayload("https://cdn.example/202.m4s", 128_000)))
        val repository = BilibiliRepository(
            client = OkHttpClient(),
            nowEpochSeconds = { 1_700_000_000L },
            apiBase = server.url("/"),
        )

        val track = repository.resolveTrack(
            source = BilibiliTrackSource(bvid = "BVQUEUE", cid = 202L),
            title = "历史主标题",
            artist = "历史作者",
            artworkUrl = "https://example.com/history.jpg",
        )

        assertEquals("长视频 · 第二首", track.title)
        assertEquals("P2 · 第二首", track.pageTitle)
        assertEquals("第二首", track.mediaText().title)
        val requests = List(3) { server.takeRequest(1, TimeUnit.SECONDS) }
        assertEquals("/x/web-interface/view", requests[0]?.requestUrl?.encodedPath)
        assertEquals("202", requests[2]?.requestUrl?.queryParameter("cid"))
    }

    private fun video() = BilibiliVideo(
        bvid = "BVQUEUE",
        aid = 1L,
        title = "列表标题",
        author = "列表作者",
        coverUrl = "https://example.com/list.jpg",
        durationSeconds = 300,
        playCount = 1L,
    )

    private fun videoDetailPayload(): String =
        """
        {
          "code": 0,
          "data": {
            "bvid": "BVQUEUE",
            "title": "长视频",
            "pic": "//i0.hdslb.com/video.jpg",
            "owner": {"name": "详情作者"},
            "pages": [
              {"cid": 101, "page": 1, "part": "第一首", "duration": 120},
              {"cid": 202, "page": 2, "part": "第二首", "duration": 180}
            ]
          }
        }
        """.trimIndent()

    private fun wbiKeyPayload(): String =
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
        """.trimIndent()

    private fun playUrlPayload(url: String, bandwidth: Long): String =
        """
        {
          "code": 0,
          "data": {
            "dash": {
              "audio": [
                {
                  "baseUrl": "$url",
                  "bandwidth": $bandwidth,
                  "codecs": "mp4a.40.2"
                }
              ]
            }
          }
        }
        """.trimIndent()

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}

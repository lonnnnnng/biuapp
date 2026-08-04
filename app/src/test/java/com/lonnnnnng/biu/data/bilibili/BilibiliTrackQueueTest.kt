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
import org.junit.Assert.assertTrue
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

    @Test
    fun `DASH缺失时使用合并MP4渐进流播放`() = runBlocking {
        server.enqueue(jsonResponse(videoDetailPayload()))
        server.enqueue(jsonResponse(wbiKeyPayload()))
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "quality": 32,
                    "format": "mp4",
                    "durl": [{"url": "https://cdn.example/merged.mp4", "size": 1024}]
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

        val track = repository.resolveTrack(video(), pageIndex = 0)

        assertEquals("https://cdn.example/merged.mp4", track.streamUrl)
        assertEquals("兼容流", track.qualityLabel)
        assertEquals("video/mp4", track.mimeType)
    }

    @Test
    fun `视频播放解析独立视频轨和最高码率AAC音频轨`() = runBlocking {
        server.enqueue(jsonResponse(wbiKeyPayload()))
        server.enqueue(jsonResponse(videoPlayUrlPayload()))
        val repository = BilibiliRepository(
            client = OkHttpClient(),
            nowEpochSeconds = { 1_700_000_000L },
            apiBase = server.url("/"),
        )

        val streams = repository.resolveVideoDownloadStreams("BVQUEUE", 202L)

        assertEquals("https://cdn.example/video-1080.m4s", streams.video.url)
        assertEquals(listOf("https://backup.example/video-1080.m4s"), streams.video.backupUrls)
        assertEquals("avc1.640028", streams.video.codecs)
        assertTrue(streams.video.qualityLabel.startsWith("1080p"))
        assertEquals("https://cdn.example/audio-high.m4s", streams.audio.url)
        assertEquals(192_000L, streams.audio.bandwidth)
        val requests = List(2) { server.takeRequest(1, TimeUnit.SECONDS) }
        assertEquals("/x/player/wbi/playurl", requests[1]?.requestUrl?.encodedPath)
        assertEquals("202", requests[1]?.requestUrl?.queryParameter("cid"))
        assertEquals("4048", requests[1]?.requestUrl?.queryParameter("fnval"))
    }

    @Test
    fun `视频播放按指定清晰度选轨并返回实际可用画质`() = runBlocking {
        server.enqueue(jsonResponse(wbiKeyPayload()))
        server.enqueue(jsonResponse(videoPlayUrlPayload()))
        val repository = BilibiliRepository(
            client = OkHttpClient(),
            nowEpochSeconds = { 1_700_000_000L },
            apiBase = server.url("/"),
        )

        val streams = repository.resolveVideoPlaybackStreams("BVQUEUE", 202L, qualityId = 64)

        assertEquals("https://cdn.example/video-720.m4s", streams.video.url)
        assertEquals(listOf(120, 80, 64), streams.availableVideos.map(DashVideoStream::qualityId))
        assertEquals(
            listOf("https://cdn.example/video-4k-hevc.m4s", "https://cdn.example/video-1080.m4s", "https://cdn.example/video-720.m4s"),
            streams.availableVideos.map(DashVideoStream::url),
        )
        assertEquals("https://cdn.example/audio-high.m4s", streams.audio.url)
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

    private fun videoPlayUrlPayload(): String =
        """
        {
          "code": 0,
          "data": {
            "dash": {
              "video": [
                {
                  "id": 120,
                  "baseUrl": "https://cdn.example/video-4k-hevc.m4s",
                  "bandwidth": 8000000,
                  "codecs": "hev1.1.6.L153.B0",
                  "width": 3840,
                  "height": 2160,
                  "frameRate": "30"
                },
                {
                  "id": 80,
                  "baseUrl": "https://cdn.example/video-1080-hevc.m4s",
                  "bandwidth": 1900000,
                  "codecs": "hev1.1.6.L120.B0",
                  "width": 1920,
                  "height": 1080,
                  "frameRate": "30"
                },
                {
                  "id": 80,
                  "baseUrl": "https://cdn.example/video-1080.m4s",
                  "backupUrl": ["https://backup.example/video-1080.m4s"],
                  "bandwidth": 2400000,
                  "codecs": "avc1.640028",
                  "width": 1920,
                  "height": 1080,
                  "frameRate": "30"
                },
                {
                  "id": 64,
                  "baseUrl": "https://cdn.example/video-720.m4s",
                  "bandwidth": 1200000,
                  "codecs": "avc1.64001f",
                  "width": 1280,
                  "height": 720,
                  "frameRate": "30"
                }
              ],
              "audio": [
                {
                  "baseUrl": "https://cdn.example/audio-low.m4s",
                  "bandwidth": 64000,
                  "codecs": "mp4a.40.2"
                },
                {
                  "baseUrl": "https://cdn.example/audio-high.m4s",
                  "bandwidth": 192000,
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

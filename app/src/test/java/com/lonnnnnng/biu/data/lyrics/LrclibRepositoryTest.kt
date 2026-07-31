package com.lonnnnnng.biu.data.lyrics

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class LrclibRepositoryTest {
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
    fun `搜索只返回包含同步歌词的结果并发送可识别UserAgent`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                [
                  {"id": 1, "trackName": "歌曲 A", "artistName": "歌手 A", "albumName": "专辑 A", "duration": 183.4, "syncedLyrics": "[00:01.00]第一句"},
                  {"id": 2, "trackName": "歌曲 B", "artistName": "歌手 B", "albumName": null, "duration": 200, "syncedLyrics": null},
                  {"id": 3, "trackName": "歌曲 C", "artistName": "歌手 C", "albumName": "", "duration": 99, "syncedLyrics": "   "}
                ]
                """.trimIndent(),
            ),
        )
        val repository = LrclibRepository(
            client = OkHttpClient(),
            apiBase = server.url("/"),
            userAgent = "BiuAndroid/test",
        )

        val results = repository.search("歌曲 A 歌手 A")

        assertEquals(listOf(1L), results.map(LyricsSearchResult::id))
        assertEquals(183, results.single().durationSeconds)
        val request = server.takeRequest(1, TimeUnit.SECONDS)
        assertEquals("/api/search", request?.requestUrl?.encodedPath)
        assertEquals("歌曲 A 歌手 A", request?.requestUrl?.queryParameter("q"))
        assertEquals("BiuAndroid/test", request?.getHeader("User-Agent"))
    }

    @Test
    fun `限流响应保留RetryAfter供界面提示`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "12"),
        )
        val repository = LrclibRepository(
            client = OkHttpClient(),
            apiBase = server.url("/"),
        )

        val error = assertThrows(LyricsRateLimitedException::class.java) {
            runBlocking { repository.search("测试") }
        }

        assertEquals(12L, error.retryAfterSeconds)
    }
}


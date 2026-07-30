package com.lonnnnnng.biu.download

import java.io.File
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ResumableFileDownloaderTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `已有断点时请求剩余字节并追加 206 响应`() = runBlocking {
        val server = MockWebServer().apply {
            enqueue(
                MockResponse()
                    .setResponseCode(206)
                    .setHeader("Content-Range", "bytes 4-9/10")
                    .setBody("efghij"),
            )
            start()
        }
        val target = File(temporaryFolder.root, "track.part").apply { writeText("abcd") }

        try {
            val size = ResumableFileDownloader(OkHttpClient()).download(
                urls = listOf(server.url("/track").toString()),
                targetFile = target,
            )

            assertEquals("bytes=4-", server.takeRequest().getHeader("Range"))
            assertEquals(10L, size)
            assertEquals("abcdefghij", target.readText())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `断点失效时只清空一次并从头重试`() = runBlocking {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setResponseCode(416))
            enqueue(MockResponse().setResponseCode(200).setBody("fresh"))
            start()
        }
        val target = File(temporaryFolder.root, "expired.part").apply { writeText("stale") }

        try {
            val size = ResumableFileDownloader(OkHttpClient()).download(
                urls = listOf(server.url("/track").toString()),
                targetFile = target,
            )

            assertEquals("bytes=5-", server.takeRequest().getHeader("Range"))
            assertEquals(null, server.takeRequest().getHeader("Range"))
            assertEquals(5L, size)
            assertEquals("fresh", target.readText())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `服务端忽略 Range 时先重置进度并覆盖旧文件`() = runBlocking {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setResponseCode(200).setBody("replacement"))
            start()
        }
        val target = File(temporaryFolder.root, "ignored-range.part").apply { writeText("old") }
        val progress = mutableListOf<Pair<Long, Long>>()

        try {
            val size = ResumableFileDownloader(OkHttpClient()).download(
                urls = listOf(server.url("/track").toString()),
                targetFile = target,
                onProgress = { downloaded, total -> progress += downloaded to total },
            )

            assertEquals("bytes=3-", server.takeRequest().getHeader("Range"))
            assertEquals(0L to 11L, progress.first())
            assertEquals(11L, size)
            assertEquals("replacement", target.readText())
        } finally {
            server.shutdown()
        }
    }
}

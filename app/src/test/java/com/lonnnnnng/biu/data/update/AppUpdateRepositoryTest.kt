package com.lonnnnnng.biu.data.update

import java.net.InetAddress
import java.net.Proxy
import kotlinx.coroutines.runBlocking
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppUpdateRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: AppUpdateRepository

    @Before
    fun setUp() {
        server = MockWebServer().also(MockWebServer::start)
        val client = OkHttpClient.Builder()
            .proxy(Proxy.NO_PROXY)
            .dns(
                object : Dns {
                    override fun lookup(hostname: String): List<InetAddress> =
                        listOf(InetAddress.getByName("127.0.0.1"))
                },
            )
            .build()
        repository = AppUpdateRepository(
            client = client,
            latestReleaseUrl = server.url("/repos/lonnnnnng/biuapp/releases/latest").toString(),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `解析最新版并选择 APK 资产`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "tag_name": "v0.1.2",
                  "name": "BiuApp 0.1.2",
                  "body": "新增在线更新",
                  "html_url": "https://github.com/lonnnnnng/biuapp/releases/tag/v0.1.2",
                  "assets": [
                    {
                      "name": "SHA256SUMS",
                      "content_type": "text/plain",
                      "browser_download_url": "https://example.com/SHA256SUMS"
                    },
                    {
                      "name": "BiuApp-v0.1.2.apk",
                      "content_type": "application/vnd.android.package-archive",
                      "browser_download_url": "https://github.com/lonnnnnng/biuapp/releases/download/v0.1.2/BiuApp-v0.1.2.apk"
                    }
                  ]
                }
                """.trimIndent(),
            ),
        )

        val update = repository.latestRelease()

        assertEquals("0.1.2", update.version)
        assertEquals("v0.1.2", update.tagName)
        assertEquals("新增在线更新", update.releaseNotes)
        assertEquals(
            "https://github.com/lonnnnnng/biuapp/releases/download/v0.1.2/BiuApp-v0.1.2.apk",
            update.downloadUrl,
        )
        val request = server.takeRequest()
        assertEquals("/repos/lonnnnnng/biuapp/releases/latest", request.path)
        assertEquals("application/vnd.github+json", request.getHeader("Accept"))
        assertEquals("BiuApp-Android", request.getHeader("User-Agent"))
    }

    @Test
    fun `缺少 APK 资产时返回明确错误`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "tag_name": "v0.1.2",
                  "body": "release",
                  "html_url": "https://example.com/release",
                  "assets": [{"name":"SHA256SUMS","browser_download_url":"https://example.com/SHA256SUMS"}]
                }
                """.trimIndent(),
            ),
        )

        val error = runCatching { repository.latestRelease() }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message.orEmpty().contains("APK"))
    }

    @Test
    fun `多个非标准 APK 资产时拒绝猜测安装包`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "tag_name": "v0.1.2",
                  "assets": [
                    {"name":"arm64.apk","browser_download_url":"https://github.com/a/arm64.apk"},
                    {"name":"x86.apk","browser_download_url":"https://github.com/a/x86.apk"}
                  ]
                }
                """.trimIndent(),
            ),
        )

        val error = runCatching { repository.latestRelease() }.exceptionOrNull()

        assertTrue(error?.message.orEmpty().contains("多个 APK"))
    }

    @Test
    fun `非 GitHub HTTPS 下载地址会被拒绝`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "tag_name": "v0.1.2",
                  "assets": [
                    {"name":"BiuApp-v0.1.2.apk","browser_download_url":"http://example.com/BiuApp.apk"}
                  ]
                }
                """.trimIndent(),
            ),
        )

        val error = runCatching { repository.latestRelease() }.exceptionOrNull()

        assertTrue(error?.message.orEmpty().contains("不受信任"))
    }
}

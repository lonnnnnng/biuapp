package com.lonnnnnng.biu.data.bilibili

import java.net.InetAddress
import java.net.Proxy
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BilibiliRequestHeadersInterceptorTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer().also(MockWebServer::start)
        client = OkHttpClient.Builder()
            .proxy(Proxy.NO_PROXY)
            .dns(
                object : Dns {
                    override fun lookup(hostname: String): List<InetAddress> =
                        listOf(InetAddress.getByName("127.0.0.1"))
                },
            )
            .addInterceptor(BilibiliRequestHeadersInterceptor(cookieProvider = { "SESSDATA=secret" }))
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `Bilibili API 请求携带账号 Cookie`() {
        execute("api.bilibili.com")

        assertEquals("SESSDATA=secret", server.takeRequest().getHeader("Cookie"))
    }

    @Test
    fun `非 Bilibili 请求不会泄露账号 Cookie`() {
        execute("example.com")

        assertNull(server.takeRequest().getHeader("Cookie"))
    }

    @Test
    fun `Bilibili CDN 携带来源头但不携带账号 Cookie`() {
        execute("upos.mcdn.bilivideo.cn")

        val request = server.takeRequest()
        assertEquals("https://www.bilibili.com/", request.getHeader("Referer"))
        assertEquals("https://www.bilibili.com", request.getHeader("Origin"))
        assertEquals(true, request.getHeader("User-Agent")?.contains("Chrome/141.0.0.0"))
        assertNull(request.getHeader("Cookie"))
    }

    private fun execute(host: String) {
        server.enqueue(MockResponse().setResponseCode(200))
        val url = server.url("/probe").newBuilder().host(host).build()
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            assertEquals(200, response.code)
        }
    }
}

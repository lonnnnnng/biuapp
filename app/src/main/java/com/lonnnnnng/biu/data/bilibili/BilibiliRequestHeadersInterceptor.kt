package com.lonnnnnng.biu.data.bilibili

import okhttp3.Interceptor
import okhttp3.Response

class BilibiliRequestHeadersInterceptor(
    private val cookieProvider: () -> String? = { null },
    private val userAgent: String = DEFAULT_USER_AGENT,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host
        if (!BilibiliDomains.isOwnedHost(host)) {
            return chain.proceed(request)
        }

        val builder = request.newBuilder()
            .header("Referer", BILIBILI_WEB_ORIGIN)
            .header("Origin", BILIBILI_WEB_ORIGIN.removeSuffix("/"))
            .header("User-Agent", userAgent)

        // 登录 Cookie 只允许发给 bilibili.com 主站域名；CDN 只携带播放所需来源头，避免凭据横向泄露。
        if (BilibiliDomains.acceptsAccountCookie(host)) {
            cookieProvider()?.takeIf(String::isNotBlank)?.let { cookie ->
                builder.header("Cookie", cookie)
            }
        }

        return chain.proceed(builder.build())
    }

    private companion object {
        const val BILIBILI_WEB_ORIGIN = "https://www.bilibili.com/"
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/138.0 Mobile Safari/537.36"
    }
}

object BilibiliDomains {
    private val ownedSuffixes = setOf("bilibili.com", "bilivideo.com", "hdslb.com")

    fun isOwnedHost(host: String): Boolean = ownedSuffixes.any { suffix ->
        host == suffix || host.endsWith(".$suffix")
    }

    fun acceptsAccountCookie(host: String): Boolean =
        host == "bilibili.com" || host.endsWith(".bilibili.com")
}

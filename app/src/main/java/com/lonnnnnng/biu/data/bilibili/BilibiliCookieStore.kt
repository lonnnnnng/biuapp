package com.lonnnnnng.biu.data.bilibili

import android.webkit.CookieManager

class BilibiliCookieStore {
    fun cookieHeader(): String? {
        return CookieManager.getInstance()
            .getCookie(BILIBILI_API_URL)
            ?.takeIf(String::isNotBlank)
    }

    fun flush() {
        CookieManager.getInstance().flush()
    }

    fun sessionVersion(): Int? = loginSessionVersion(cookieHeader())

    fun clear(onComplete: (Boolean) -> Unit) {
        CookieManager.getInstance().removeAllCookies(onComplete)
    }

    companion object {
        const val BILIBILI_API_URL = "https://api.bilibili.com"
        const val BILIBILI_LOGIN_URL = "https://passport.bilibili.com/h5-app/passport/login"
    }
}

internal fun loginSessionVersion(cookieHeader: String?): Int? {
    return cookieHeader
        ?.split(';')
        ?.asSequence()
        ?.map(String::trim)
        ?.firstOrNull { cookie -> cookie.substringBefore('=') == "SESSDATA" }
        ?.substringAfter('=', "")
        ?.takeIf(String::isNotBlank)
        ?.hashCode()
}

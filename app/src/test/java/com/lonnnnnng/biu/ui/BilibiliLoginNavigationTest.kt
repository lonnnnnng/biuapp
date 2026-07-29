package com.lonnnnnng.biu.ui

import com.lonnnnnng.biu.data.bilibili.BilibiliCookieStore
import com.lonnnnnng.biu.data.bilibili.BILIBILI_WEB_USER_AGENT
import com.lonnnnnng.biu.data.bilibili.loginSessionVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BilibiliLoginNavigationTest {
    @Test
    fun `登录页面只允许 HTTPS Bilibili 导航`() {
        assertTrue(BilibiliLoginNavigation.isAllowed("https://passport.bilibili.com/login"))
        assertTrue(BilibiliLoginNavigation.isAllowed("https://www.bilibili.com/"))
        assertFalse(BilibiliLoginNavigation.isAllowed("http://passport.bilibili.com/login"))
        assertFalse(BilibiliLoginNavigation.isAllowed("https://bilibili.com.example.org/login"))
    }

    @Test
    fun `登录入口使用移动端 H5 页面`() {
        assertEquals(
            "https://passport.bilibili.com/h5-app/passport/login",
            BilibiliCookieStore.BILIBILI_LOGIN_URL,
        )
    }

    @Test
    fun `播放请求继续使用桌面 Web UA`() {
        assertTrue(BILIBILI_WEB_USER_AGENT.contains("Windows NT 10.0"))
        assertFalse(BILIBILI_WEB_USER_AGENT.contains("; wv"))
    }

    @Test
    fun `H5 登录页使用实际视口高度修复 WebView 布局`() {
        assertTrue(BILIBILI_LOGIN_VIEWPORT_FIX.contains("window.visualViewport?.height"))
        assertTrue(BILIBILI_LOGIN_VIEWPORT_FIX.contains(".login-wrap{min-height:"))
        assertFalse(BILIBILI_LOGIN_VIEWPORT_FIX.contains("checked = true"))
    }

    @Test
    fun `登录会话只识别非空 SESSDATA`() {
        assertNull(loginSessionVersion(null))
        assertNull(loginSessionVersion("bili_jct=csrf; DedeUserID=123"))
        assertNull(loginSessionVersion("SESSDATA=; bili_jct=csrf"))
        assertEquals("session-token".hashCode(), loginSessionVersion("foo=1; SESSDATA=session-token; bili_jct=csrf"))
    }
}

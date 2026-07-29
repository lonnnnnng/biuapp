package com.lonnnnnng.biu.ui

import org.junit.Assert.assertFalse
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
}

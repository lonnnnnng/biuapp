package com.lonnnnnng.biu.data.bilibili

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BilibiliCookieParserTest {
    @Test
    fun `从Cookie头读取bili jct作为CSRF`() {
        assertEquals("token%2Fvalue", csrfToken("SESSDATA=session; bili_jct=token%2Fvalue; DedeUserID=7"))
    }

    @Test
    fun `缺少或空bili jct时不返回CSRF`() {
        assertNull(csrfToken("SESSDATA=session"))
        assertNull(csrfToken("bili_jct="))
        assertNull(csrfToken(null))
    }
}

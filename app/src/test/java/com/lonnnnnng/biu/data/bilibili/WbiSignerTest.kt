package com.lonnnnnng.biu.data.bilibili

import org.junit.Assert.assertEquals
import org.junit.Test

class WbiSignerTest {
    @Test
    fun `固定参数生成与桌面实现一致的签名`() {
        val result = WbiSigner.sign(
            parameters = mapOf("foo" to 114, "bar" to 514, "baz" to 1919810),
            imgKey = "7cd084941338484aae1ad9425b84077c",
            subKey = "4932caff0ff746eab6f01bf08b70ac45",
            timestampSeconds = 1702204169L,
        )

        assertEquals("6149fdadf571698ca7e6a567265cd0ee", result.parameters["w_rid"])
        assertEquals(
            "bar=514&baz=1919810&foo=114&wts=1702204169&w_rid=6149fdadf571698ca7e6a567265cd0ee",
            result.encodedQuery,
        )
    }
}

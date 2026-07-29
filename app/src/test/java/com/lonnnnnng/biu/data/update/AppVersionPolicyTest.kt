package com.lonnnnnng.biu.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class AppVersionPolicyTest {
    @Test
    fun `带 v 前缀的新补丁版本可识别`() {
        assertTrue(AppVersionPolicy.isNewer("v0.1.2", "0.1.1"))
    }

    @Test
    fun `相同版本即使前缀不同也不是更新`() {
        assertFalse(AppVersionPolicy.isNewer("0.1.1", "v0.1.1"))
    }

    @Test
    fun `次版本按数字比较而不是字符串比较`() {
        assertTrue(AppVersionPolicy.isNewer("0.2.0", "0.1.9"))
    }

    @Test
    fun `主版本升级可识别`() {
        assertTrue(AppVersionPolicy.isNewer("1.0.0", "0.9.9"))
    }

    @Test
    fun `超大数字版本仍可正确比较`() {
        assertTrue(AppVersionPolicy.isNewer("1.999999999999999999999", "1.2.0"))
    }

    @Test
    fun `非数字版本号会被拒绝`() {
        assertThrows(IllegalArgumentException::class.java) {
            AppVersionPolicy.isNewer("release", "0.1.1")
        }
    }
}

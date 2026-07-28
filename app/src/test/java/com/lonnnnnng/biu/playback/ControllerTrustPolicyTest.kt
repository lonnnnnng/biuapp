package com.lonnnnnng.biu.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControllerTrustPolicyTest {
    private val policy = ControllerTrustPolicy(
        applicationPackage = "com.lonnnnnng.biu",
        applicationUid = 10123,
    )

    @Test
    fun `同包名同 UID 的应用控制器可以连接`() {
        assertTrue(policy.isAllowed("com.lonnnnnng.biu", 10123, isSystemTrusted = false))
    }

    @Test
    fun `系统可信控制器可以跨进程连接`() {
        assertTrue(policy.isAllowed("com.android.systemui", 1000, isSystemTrusted = true))
    }

    @Test
    fun `普通第三方控制器不能连接`() {
        assertFalse(policy.isAllowed("com.example.remote", 10456, isSystemTrusted = false))
    }
}

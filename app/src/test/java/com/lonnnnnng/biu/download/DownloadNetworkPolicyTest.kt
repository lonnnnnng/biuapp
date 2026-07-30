package com.lonnnnnng.biu.download

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadNetworkPolicyTest {
    @Test
    fun `任意网络仍需通过系统互联网验证`() {
        assertTrue(
            DownloadNetworkPolicy.isAllowed(
                DownloadNetworkPreference.ANY_VALIDATED,
                DownloadNetworkState(hasInternet = true, validated = true, unmetered = false),
            ),
        )
        assertFalse(
            DownloadNetworkPolicy.isAllowed(
                DownloadNetworkPreference.ANY_VALIDATED,
                DownloadNetworkState(hasInternet = true, validated = false, unmetered = true),
            ),
        )
    }

    @Test
    fun `仅非计费网络拒绝已验证的计费连接`() {
        assertFalse(
            DownloadNetworkPolicy.isAllowed(
                DownloadNetworkPreference.UNMETERED_ONLY,
                DownloadNetworkState(hasInternet = true, validated = true, unmetered = false),
            ),
        )
        assertTrue(
            DownloadNetworkPolicy.isAllowed(
                DownloadNetworkPreference.UNMETERED_ONLY,
                DownloadNetworkState(hasInternet = true, validated = true, unmetered = true),
            ),
        )
    }

    @Test
    fun `没有默认互联网网络时所有策略都等待`() {
        val disconnected = DownloadNetworkState(hasInternet = false, validated = false, unmetered = false)

        assertFalse(DownloadNetworkPolicy.isAllowed(DownloadNetworkPreference.ANY_VALIDATED, disconnected))
        assertFalse(DownloadNetworkPolicy.isAllowed(DownloadNetworkPreference.UNMETERED_ONLY, disconnected))
    }
}

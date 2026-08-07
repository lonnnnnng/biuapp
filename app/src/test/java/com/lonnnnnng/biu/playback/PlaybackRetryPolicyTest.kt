package com.lonnnnnng.biu.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackRetryPolicyTest {
    @Test
    fun `播放地址恢复限制次数并使用短退避`() {
        assertEquals(3, PlaybackRetryPolicy.MAX_STREAM_RECOVERY_ATTEMPTS)
        assertEquals(0L, PlaybackRetryPolicy.delayMs(0))
        assertEquals(500L, PlaybackRetryPolicy.delayMs(1))
        assertEquals(1_500L, PlaybackRetryPolicy.delayMs(2))
        assertEquals(1_500L, PlaybackRetryPolicy.delayMs(8))
    }
}

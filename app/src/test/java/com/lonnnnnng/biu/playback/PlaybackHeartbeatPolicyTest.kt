package com.lonnnnnng.biu.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackHeartbeatPolicyTest {
    @Test
    fun `普通心跳三十秒内节流而动作事件强制发送`() {
        assertTrue(PlaybackHeartbeatPolicy.shouldSend(PlaybackHeartbeatEvent.PROGRESS, 100L, null))
        assertFalse(PlaybackHeartbeatPolicy.shouldSend(PlaybackHeartbeatEvent.PROGRESS, 129L, 100L))
        assertTrue(PlaybackHeartbeatPolicy.shouldSend(PlaybackHeartbeatEvent.PROGRESS, 130L, 100L))
        assertTrue(PlaybackHeartbeatPolicy.shouldSend(PlaybackHeartbeatEvent.START, 101L, 100L))
        assertTrue(PlaybackHeartbeatPolicy.shouldSend(PlaybackHeartbeatEvent.PAUSE, 101L, 100L))
        assertTrue(PlaybackHeartbeatPolicy.shouldSend(PlaybackHeartbeatEvent.END, 101L, 100L))
    }

    @Test
    fun `只有开启设置且存在Bilibili来源才允许上报`() {
        assertTrue(PlaybackHeartbeatPolicy.isReportable(enabled = true, hasBilibiliSource = true))
        assertFalse(PlaybackHeartbeatPolicy.isReportable(enabled = false, hasBilibiliSource = true))
        assertFalse(PlaybackHeartbeatPolicy.isReportable(enabled = true, hasBilibiliSource = false))
    }
}

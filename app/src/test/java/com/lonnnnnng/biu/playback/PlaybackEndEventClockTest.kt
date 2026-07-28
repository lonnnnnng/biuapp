package com.lonnnnnng.biu.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackEndEventClockTest {
    @Test
    fun `每次结束事件获得新的单调递增 ID`() {
        val clock = PlaybackEndEventClock()

        assertEquals(1L, clock.recordEnded())
        assertEquals(2L, clock.recordEnded())
        assertEquals(2L, clock.latest())
    }
}

package com.lonnnnnng.biu.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackResumePolicyTest {
    @Test
    fun `keeps position when track has meaningful remainder`() {
        assertEquals(60_000L, PlaybackResumePolicy.startPositionMs(60_000L, 180_000L))
    }

    @Test
    fun `restarts track when saved position is near the end`() {
        assertEquals(0L, PlaybackResumePolicy.startPositionMs(175_000L, 180_000L))
    }

    @Test
    fun `keeps positive position when duration is unknown`() {
        assertEquals(45_000L, PlaybackResumePolicy.startPositionMs(45_000L, 0L))
    }
}

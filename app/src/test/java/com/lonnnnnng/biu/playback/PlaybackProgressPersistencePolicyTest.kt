package com.lonnnnnng.biu.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackProgressPersistencePolicyTest {
    @Test
    fun `跨分P时保存旧媒体项进度`() {
        assertTrue(PlaybackProgressPersistencePolicy.shouldPersistTransition("BV1:101", "BV1:202"))
        assertFalse(PlaybackProgressPersistencePolicy.shouldPersistTransition("BV1:101", "BV1:101"))
        assertFalse(PlaybackProgressPersistencePolicy.shouldPersistTransition("", "BV1:202"))
    }

    @Test
    fun `未知总时长时使用旧项最终位置`() {
        assertEquals(120_000L, PlaybackProgressPersistencePolicy.durationMs(120_000L, 119_900L))
        assertEquals(4_800L, PlaybackProgressPersistencePolicy.durationMs(-9223372036854775807L, 4_800L))
        assertEquals(0L, PlaybackProgressPersistencePolicy.durationMs(-1L, -100L))
    }
}

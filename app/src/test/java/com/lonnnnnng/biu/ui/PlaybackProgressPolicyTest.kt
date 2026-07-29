package com.lonnnnnng.biu.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackProgressPolicyTest {
    @Test
    fun `已知时长时进度和缓冲限制在媒体范围内`() {
        val progress = PlaybackProgressPolicy.normalize(
            positionMs = 125_000L,
            durationMs = 120_000L,
            bufferedPositionMs = 150_000L,
            isSeekable = true,
        )

        assertEquals(120_000L, progress.positionMs)
        assertEquals(120_000L, progress.durationMs)
        assertEquals(120_000L, progress.bufferedPositionMs)
        assertTrue(progress.isSeekable)
        assertEquals(1f, progress.fraction, 0.0001f)
    }

    @Test
    fun `未知时长时禁用拖动并保留非负位置`() {
        val progress = PlaybackProgressPolicy.normalize(
            positionMs = 8_000L,
            durationMs = -9223372036854775807L,
            bufferedPositionMs = -1L,
            isSeekable = true,
        )

        assertEquals(8_000L, progress.positionMs)
        assertEquals(0L, progress.durationMs)
        assertEquals(0L, progress.bufferedPositionMs)
        assertFalse(progress.isSeekable)
        assertEquals(0f, progress.fraction, 0.0001f)
    }

    @Test
    fun `拖动比例生成范围内的毫秒位置`() {
        assertEquals(30_000L, PlaybackProgressPolicy.seekPositionMs(0.25f, 120_000L))
        assertEquals(0L, PlaybackProgressPolicy.seekPositionMs(-1f, 120_000L))
        assertEquals(120_000L, PlaybackProgressPolicy.seekPositionMs(2f, 120_000L))
        assertEquals(0L, PlaybackProgressPolicy.seekPositionMs(0.5f, 0L))
    }
}

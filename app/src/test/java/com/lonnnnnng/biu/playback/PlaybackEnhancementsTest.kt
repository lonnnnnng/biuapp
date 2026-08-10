package com.lonnnnnng.biu.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackEnhancementsTest {
    @Test
    fun volumeAtInterpolatesAndClamps() {
        assertEquals(0f, PlaybackFadePolicy.volumeAt(0f, 1f, 0, 8))
        assertEquals(0.5f, PlaybackFadePolicy.volumeAt(0f, 1f, 4, 8))
        assertEquals(1f, PlaybackFadePolicy.volumeAt(0f, 1f, 8, 8))
        assertEquals(1f, PlaybackFadePolicy.volumeAt(-1f, 2f, 8, 8))
    }

    @Test
    fun volumeBalanceModeFallsBackToOffForUnknownValues() {
        assertEquals(VolumeBalanceMode.OFF, VolumeBalanceMode.fromStoredValue("unknown"))
        assertEquals(VolumeBalanceMode.STANDARD, VolumeBalanceMode.fromStoredValue("STANDARD"))
    }

    @Test
    fun balanceProfilesIncreaseCompressionAsStrengthGrows() {
        assertTrue(VolumeBalanceMode.GENTLE.ratio < VolumeBalanceMode.STANDARD.ratio)
        assertTrue(VolumeBalanceMode.STANDARD.ratio < VolumeBalanceMode.STRONG.ratio)
        assertTrue(VolumeBalanceMode.GENTLE.thresholdDb > VolumeBalanceMode.STRONG.thresholdDb)
    }
}

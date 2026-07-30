package com.lonnnnnng.biu.playback

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackPreferencesPolicyTest {
    @Test
    fun playbackModesMapFromMedia3State() {
        assertEquals(PlaybackMode.SEQUENTIAL, PlaybackMode.fromPlayer(Player.REPEAT_MODE_OFF, false))
        assertEquals(PlaybackMode.REPEAT_ALL, PlaybackMode.fromPlayer(Player.REPEAT_MODE_ALL, false))
        assertEquals(PlaybackMode.SHUFFLE, PlaybackMode.fromPlayer(Player.REPEAT_MODE_ALL, true))
        assertEquals(PlaybackMode.REPEAT_ONE, PlaybackMode.fromPlayer(Player.REPEAT_MODE_ONE, false))
    }

    @Test
    fun playbackModeCyclesThroughFourUserVisibleStates() {
        assertEquals(PlaybackMode.REPEAT_ALL, PlaybackMode.SEQUENTIAL.next())
        assertEquals(PlaybackMode.SHUFFLE, PlaybackMode.REPEAT_ALL.next())
        assertEquals(PlaybackMode.REPEAT_ONE, PlaybackMode.SHUFFLE.next())
        assertEquals(PlaybackMode.SEQUENTIAL, PlaybackMode.REPEAT_ONE.next())
    }

    @Test
    fun speedPolicyUsesNearestSupportedValueAndSafeDefault() {
        assertEquals(1.25f, PlaybackSpeedPolicy.normalize(1.3f))
        assertEquals(PlaybackSpeedPolicy.DEFAULT, PlaybackSpeedPolicy.normalize(Float.NaN))
    }
}

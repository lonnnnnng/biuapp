package com.lonnnnnng.biu.playback

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackPreferencesPolicyTest {
    @Test
    fun playbackHistoryReportingDefaultsToEnabled() {
        assertEquals(true, PlaybackPreferences().reportPlayHistory)
    }

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

    @Test
    fun sleepTimerDefaultsToOffAndBuildsPersistentDeadline() {
        assertEquals(SleepTimerMode.OFF, PlaybackPreferences().sleepTimerMode)
        assertEquals(1_900_000L, SleepTimerPolicy.deadlineAfterMinutes(1_000_000L, 15))
        assertEquals(500L, SleepTimerPolicy.remainingMs(1_500L, 1_000L))
        assertEquals(0L, SleepTimerPolicy.remainingMs(500L, 1_000L))
    }

    @Test
    fun sleepTimerEndModesCoverNaturalEndAndRepeatTransition() {
        assertEquals(true, SleepTimerPolicy.shouldFinishOnPlaybackEnded(SleepTimerMode.TRACK_END))
        assertEquals(true, SleepTimerPolicy.shouldFinishOnPlaybackEnded(SleepTimerMode.QUEUE_END))
        assertEquals(
            true,
            SleepTimerPolicy.shouldFinishAfterAutoTransition(SleepTimerMode.TRACK_END, 1, 3),
        )
        assertEquals(
            false,
            SleepTimerPolicy.shouldFinishAfterAutoTransition(SleepTimerMode.QUEUE_END, 1, 3),
        )
        assertEquals(
            true,
            SleepTimerPolicy.shouldFinishAfterAutoTransition(SleepTimerMode.QUEUE_END, 3, 3),
        )
    }
}

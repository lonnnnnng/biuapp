package com.lonnnnnng.biu.playback

internal object PlaybackRetryPolicy {
    const val MAX_STREAM_RECOVERY_ATTEMPTS = 3

    fun delayMs(attempt: Int): Long = when (attempt.coerceAtLeast(0)) {
        0 -> 0L
        1 -> 500L
        else -> 1_500L
    }
}

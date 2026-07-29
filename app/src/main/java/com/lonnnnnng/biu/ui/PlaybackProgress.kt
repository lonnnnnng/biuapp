package com.lonnnnnng.biu.ui

import kotlin.math.roundToLong

internal data class PlaybackProgress(
    val positionMs: Long,
    val durationMs: Long,
    val bufferedPositionMs: Long,
    val isSeekable: Boolean,
) {
    val fraction: Float
        get() = if (durationMs > 0L) positionMs.toFloat() / durationMs.toFloat() else 0f

    val bufferedFraction: Float
        get() = if (durationMs > 0L) bufferedPositionMs.toFloat() / durationMs.toFloat() else 0f
}

internal object PlaybackProgressPolicy {
    fun normalize(
        positionMs: Long,
        durationMs: Long,
        bufferedPositionMs: Long,
        isSeekable: Boolean,
    ): PlaybackProgress {
        val validDurationMs = durationMs.takeIf { it > 0L } ?: 0L
        val validPositionMs = positionMs.coerceAtLeast(0L).let { position ->
            if (validDurationMs > 0L) position.coerceAtMost(validDurationMs) else position
        }
        val validBufferedPositionMs = if (validDurationMs > 0L) {
            bufferedPositionMs.coerceIn(0L, validDurationMs)
        } else {
            0L
        }
        return PlaybackProgress(
            positionMs = validPositionMs,
            durationMs = validDurationMs,
            bufferedPositionMs = validBufferedPositionMs,
            isSeekable = isSeekable && validDurationMs > 0L,
        )
    }

    fun seekPositionMs(fraction: Float, durationMs: Long): Long {
        if (durationMs <= 0L) return 0L
        return (fraction.coerceIn(0f, 1f) * durationMs).roundToLong()
    }
}

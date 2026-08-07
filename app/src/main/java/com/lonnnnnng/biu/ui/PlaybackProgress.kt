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

/**
 * 播放进度条拖动时的精细定位换算。
 *
 * long: 点击轨道仍由界面直接定位；真正拖动时按灵敏度缩小位移，避免长视频里手指轻微移动就跨越大量内容。
 */
internal object PlaybackSliderDragPolicy {
    fun dragAnchorFraction(
        currentFraction: Float,
        touchFraction: Float,
        isDragging: Boolean,
    ): Float {
        // long: 连续拖动必须从当前播放点开始，轻点则仍定位到手指落点，避免两种交互互相影响。
        return (if (isDragging) currentFraction else touchFraction).coerceIn(0f, 1f)
    }

    fun adjustedFraction(
        startFraction: Float,
        dragDistancePx: Float,
        trackWidthPx: Float,
        sensitivity: Float,
    ): Float {
        if (trackWidthPx <= 0f) return startFraction.coerceIn(0f, 1f)
        val progressDelta = dragDistancePx / trackWidthPx * sensitivity.coerceIn(0f, 1f)
        return (startFraction + progressDelta).coerceIn(0f, 1f)
    }
}

/**
 * 视频画面横向滑动的定位换算。
 *
 * long: 画面上的滑动只在手指离开时提交给播放器；这里先把位移稳定换算成目标时间，
 * 使小窗和全屏共享相同行为，也避免播放器在每个触点事件都重新缓冲。
 */
internal object VideoSwipeSeekPolicy {
    fun targetPositionMs(
        startPositionMs: Long,
        durationMs: Long,
        isSeekable: Boolean,
        dragDistancePx: Float,
        surfaceWidthPx: Float,
    ): Long? {
        if (!isSeekable || durationMs <= 0L || surfaceWidthPx <= 0f) return null
        val normalizedStartPositionMs = startPositionMs.coerceIn(0L, durationMs)
        val progressDelta = (dragDistancePx / surfaceWidthPx).coerceIn(-1f, 1f)
        return (normalizedStartPositionMs + durationMs * progressDelta)
            .roundToLong()
            .coerceIn(0L, durationMs)
    }
}

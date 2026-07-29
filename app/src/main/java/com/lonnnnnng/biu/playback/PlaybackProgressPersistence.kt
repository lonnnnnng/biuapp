package com.lonnnnnng.biu.playback

internal object PlaybackProgressPersistencePolicy {
    fun shouldPersistTransition(oldMediaId: String, newMediaId: String): Boolean {
        return oldMediaId.isNotBlank() && oldMediaId != newMediaId
    }

    fun durationMs(timelineDurationMs: Long, positionMs: Long): Long {
        // long: 极短分 P 可能在 Media3 报出总时长前就切换，至少以旧项最终位置作为可恢复的总时长下界。
        return timelineDurationMs.takeIf { it > 0L } ?: positionMs.coerceAtLeast(0L)
    }
}

package com.lonnnnnng.biu.playback

enum class PlaybackHeartbeatEvent(val playType: Int) {
    PROGRESS(0),
    START(1),
    PAUSE(2),
    END(4),
}

object PlaybackHeartbeatPolicy {
    private const val INTERVAL_SECONDS = 30L

    fun shouldSend(event: PlaybackHeartbeatEvent, nowEpochSeconds: Long, lastSentAtEpochSeconds: Long?): Boolean {
        return event != PlaybackHeartbeatEvent.PROGRESS ||
            lastSentAtEpochSeconds == null ||
            nowEpochSeconds - lastSentAtEpochSeconds >= INTERVAL_SECONDS
    }

    fun isReportable(enabled: Boolean, hasBilibiliSource: Boolean): Boolean =
        enabled && hasBilibiliSource
}

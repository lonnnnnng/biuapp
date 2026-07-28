package com.lonnnnnng.biu.playback

import java.util.concurrent.atomic.AtomicLong

class PlaybackEndEventClock {
    private val latestEventId = AtomicLong(0L)

    fun recordEnded(): Long = latestEventId.incrementAndGet()

    fun latest(): Long = latestEventId.get()
}

package com.lonnnnnng.biu.playback

import android.os.Bundle
import androidx.media3.session.SessionCommand
import com.lonnnnnng.biu.core.model.PlaybackMediaMode

object PlaybackSessionCommands {
    val setMediaMode = SessionCommand(ACTION_SET_MEDIA_MODE, Bundle.EMPTY)
    val setVideoQuality = SessionCommand(ACTION_SET_VIDEO_QUALITY, Bundle.EMPTY)

    fun mediaModeArguments(mode: PlaybackMediaMode): Bundle {
        return Bundle().apply { putString(ARG_MEDIA_MODE, mode.name) }
    }

    fun requestedMediaMode(arguments: Bundle): PlaybackMediaMode? {
        val storedValue = arguments.getString(ARG_MEDIA_MODE) ?: return null
        return PlaybackMediaMode.entries.firstOrNull { mode -> mode.name == storedValue }
    }

    fun videoQualityArguments(qualityId: Int): Bundle {
        return Bundle().apply { putInt(ARG_VIDEO_QUALITY_ID, qualityId) }
    }

    fun requestedVideoQualityId(arguments: Bundle): Int? {
        return arguments.getInt(ARG_VIDEO_QUALITY_ID, 0).takeIf { it > 0 }
    }

    fun resultExtras(message: String): Bundle {
        return Bundle().apply { putString(RESULT_MESSAGE, message) }
    }

    fun resultMessage(arguments: Bundle): String? {
        return arguments.getString(RESULT_MESSAGE)?.takeIf(String::isNotBlank)
    }

    private const val ACTION_SET_MEDIA_MODE = "com.lonnnnnng.biu.playback.SET_MEDIA_MODE"
    private const val ACTION_SET_VIDEO_QUALITY = "com.lonnnnnng.biu.playback.SET_VIDEO_QUALITY"
    private const val ARG_MEDIA_MODE = "media_mode"
    private const val ARG_VIDEO_QUALITY_ID = "video_quality_id"
    private const val RESULT_MESSAGE = "message"
}

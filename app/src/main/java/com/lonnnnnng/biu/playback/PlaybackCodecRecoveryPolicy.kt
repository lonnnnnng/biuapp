package com.lonnnnnng.biu.playback

import androidx.media3.common.PlaybackException

internal class PlaybackCodecRecoveryPolicy {
    private var recoveryInFlight = false
    private var activeMediaId: String? = null
    private val recoveryStartedAtMs = mutableListOf<Long>()

    fun tryBeginRecovery(
        mediaId: String,
        errorCode: Int,
        causeTypeNames: List<String>,
        nowElapsedMs: Long,
        isMtkVideoPlayback: Boolean = false,
    ): Boolean {
        if (!isRecoverableCodecFailure(errorCode, causeTypeNames, isMtkVideoPlayback)) return false
        if (recoveryInFlight) return false
        if (activeMediaId != mediaId) {
            activeMediaId = mediaId
            recoveryStartedAtMs.clear()
        }
        recoveryStartedAtMs.removeAll { startedAt -> nowElapsedMs - startedAt >= RECOVERY_WINDOW_MS }
        if (recoveryStartedAtMs.size >= MAX_RECOVERIES_PER_WINDOW) return false
        recoveryInFlight = true
        recoveryStartedAtMs += nowElapsedMs
        return true
    }

    fun finishRecovery() {
        recoveryInFlight = false
    }

    fun resetForExternalMediaItemTransition() {
        if (!recoveryInFlight) {
            activeMediaId = null
            recoveryStartedAtMs.clear()
        }
    }

    private fun isRecoverableCodecFailure(
        errorCode: Int,
        causeTypeNames: List<String>,
        isMtkVideoPlayback: Boolean,
    ): Boolean {
        // long: 厂商 mediaserver 死亡有时只上报 UNSPECIFIED；保留 CodecException 类型判断才能覆盖真实 MTK DEAD_OBJECT 链路。
        return errorCode in RECOVERABLE_DECODER_ERROR_CODES ||
            causeTypeNames.any { typeName -> typeName == MEDIA_CODEC_EXCEPTION_TYPE } ||
            (isMtkVideoPlayback && errorCode == PlaybackException.ERROR_CODE_UNSPECIFIED)
    }

    private companion object {
        const val MEDIA_CODEC_EXCEPTION_TYPE = "android.media.MediaCodec\$CodecException"
        val RECOVERABLE_DECODER_ERROR_CODES = setOf(
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
        )
        const val RECOVERY_WINDOW_MS = 30_000L
        const val MAX_RECOVERIES_PER_WINDOW = 2
    }
}

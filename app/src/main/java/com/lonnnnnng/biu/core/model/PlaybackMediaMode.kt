package com.lonnnnnng.biu.core.model

enum class PlaybackMediaMode {
    AUDIO,
    VIDEO,
    ;

    companion object {
        fun fromStoredValue(value: String?): PlaybackMediaMode {
            return entries.firstOrNull { mode -> mode.name.equals(value, ignoreCase = true) } ?: AUDIO
        }
    }
}

data class PlaybackVideoQuality(
    val qualityId: Int,
    val label: String,
)

data class PlaybackStreamMetadata(
    val mode: PlaybackMediaMode,
    val audioUrl: String,
    val videoUrl: String?,
    val audioQualityLabel: String?,
    val videoQualityLabel: String?,
    val selectedVideoQualityId: Int?,
    val availableVideoQualities: List<PlaybackVideoQuality>,
) {
    init {
        require(audioUrl.isNotBlank()) { "音频播放地址不能为空" }
        require(mode != PlaybackMediaMode.VIDEO || !videoUrl.isNullOrBlank()) { "视频播放地址不能为空" }
    }

    val playbackUrl: String
        get() = if (mode == PlaybackMediaMode.VIDEO) videoUrl.orEmpty() else audioUrl

    // long: 视频模式只属于当前内存播放会话；队列落库始终保存音频地址，保证进程重启后仍按产品默认值恢复为音频。
    val persistentAudioUrl: String
        get() = audioUrl

    val displayQualityLabel: String?
        get() = if (mode == PlaybackMediaMode.VIDEO) videoQualityLabel else audioQualityLabel

    companion object {
        fun audio(audioUrl: String, qualityLabel: String?): PlaybackStreamMetadata {
            return PlaybackStreamMetadata(
                mode = PlaybackMediaMode.AUDIO,
                audioUrl = audioUrl,
                videoUrl = null,
                audioQualityLabel = qualityLabel,
                videoQualityLabel = null,
                selectedVideoQualityId = null,
                availableVideoQualities = emptyList(),
            )
        }

        fun video(
            audioUrl: String,
            videoUrl: String,
            audioQualityLabel: String?,
            videoQualityLabel: String?,
            selectedVideoQualityId: Int? = null,
            availableVideoQualities: List<PlaybackVideoQuality> = emptyList(),
        ): PlaybackStreamMetadata {
            return PlaybackStreamMetadata(
                mode = PlaybackMediaMode.VIDEO,
                audioUrl = audioUrl,
                videoUrl = videoUrl,
                audioQualityLabel = audioQualityLabel,
                videoQualityLabel = videoQualityLabel,
                selectedVideoQualityId = selectedVideoQualityId,
                availableVideoQualities = availableVideoQualities,
            )
        }
    }
}

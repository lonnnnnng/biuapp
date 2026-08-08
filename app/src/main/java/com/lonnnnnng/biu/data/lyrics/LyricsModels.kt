package com.lonnnnnng.biu.data.lyrics

import com.lonnnnnng.biu.core.model.BilibiliTrackSource

data class LyricsLine(
    val startTimeMs: Long,
    val text: String,
    val translation: String? = null,
)

enum class LyricsTextSize(
    val label: String,
    val multiplier: Float,
) {
    SMALL("小", 0.88f),
    STANDARD("标准", 1f),
    LARGE("大", 1.18f),
    ;

    companion object {
        fun fromStoredValue(value: String?): LyricsTextSize {
            // long: 歌词字号配置损坏时回退到标准档，避免异常倍率让整页歌词无法阅读或操作。
            return entries.firstOrNull { size -> size.name == value } ?: STANDARD
        }
    }
}

object LyricsOffsetPolicy {
    const val STEP_MS = 500L
    const val MIN_OFFSET_MS = -10_000L
    const val MAX_OFFSET_MS = 10_000L

    fun normalize(offsetMs: Long): Long = offsetMs.coerceIn(MIN_OFFSET_MS, MAX_OFFSET_MS)

    fun adjust(currentOffsetMs: Long, deltaMs: Long): Long = normalize(currentOffsetMs + deltaMs)
}

enum class LyricsSource {
    LRCLIB,
}

data class LyricsDocument(
    val cacheKey: String,
    val source: LyricsSource,
    val lines: List<LyricsLine>,
    val rawLyrics: String,
    val providerId: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val isUserSelected: Boolean = false,
    val offsetMs: Long = 0L,
)

data class LyricsSearchResult(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val albumName: String?,
    val durationSeconds: Int,
    val syncedLyrics: String,
)

fun lyricsCacheKey(source: BilibiliTrackSource?, mediaId: String): String {
    return source?.let { "${it.bvid}:${it.cid}" } ?: "media:$mediaId"
}

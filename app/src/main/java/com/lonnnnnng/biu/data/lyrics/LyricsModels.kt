package com.lonnnnnng.biu.data.lyrics

data class LyricsLine(
    val startTimeMs: Long,
    val text: String,
)

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
)

data class LyricsSearchResult(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val albumName: String?,
    val durationSeconds: Int,
    val syncedLyrics: String,
)

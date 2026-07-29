package com.lonnnnnng.biu.core.model

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val streamUrl: String,
    val artworkUrl: String? = null,
    val qualityLabel: String? = null,
    val source: BilibiliTrackSource? = null,
)

enum class AudioQualityPreference(val label: String) {
    HIGHEST("最高音质"),
    DATA_SAVER("省流量"),
}

data class BilibiliTrackSource(
    val bvid: String,
    val cid: Long,
    val qualityPreference: AudioQualityPreference = AudioQualityPreference.HIGHEST,
)

fun Track.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(streamUrl.toUri())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setArtworkUri(artworkUrl?.toUri())
                .setDescription(qualityLabel)
                .setExtras(source?.toExtras())
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build(),
        )
        .build()
}

fun MediaItem.bilibiliSource(): BilibiliTrackSource? {
    val extras = mediaMetadata.extras ?: return null
    val bvid = extras.getString(EXTRA_BVID)?.takeIf(String::isNotBlank) ?: return null
    val cid = extras.getLong(EXTRA_CID, 0L).takeIf { it > 0L } ?: return null
    val qualityPreference = extras.getString(EXTRA_QUALITY_PREFERENCE)
        ?.let { value -> runCatching { AudioQualityPreference.valueOf(value) }.getOrNull() }
        ?: AudioQualityPreference.HIGHEST
    return BilibiliTrackSource(bvid, cid, qualityPreference)
}

private fun BilibiliTrackSource.toExtras(): Bundle {
    return Bundle().apply {
        putString(EXTRA_BVID, bvid)
        putLong(EXTRA_CID, cid)
        putString(EXTRA_QUALITY_PREFERENCE, qualityPreference.name)
    }
}

private const val EXTRA_BVID = "biu.bilibili.bvid"
private const val EXTRA_CID = "biu.bilibili.cid"
private const val EXTRA_QUALITY_PREFERENCE = "biu.bilibili.quality_preference"

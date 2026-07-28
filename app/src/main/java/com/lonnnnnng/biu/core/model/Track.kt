package com.lonnnnnng.biu.core.model

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val streamUrl: String,
)

fun Track.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(streamUrl.toUri())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build(),
        )
        .build()
}

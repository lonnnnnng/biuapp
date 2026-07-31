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
    val pageTitle: String? = null,
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
    val aid: Long? = null,
)

fun Track.toMediaItem(): MediaItem {
    val mediaText = mediaText()
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(streamUrl.toUri())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(mediaText.title)
                .setAlbumTitle(mediaText.albumTitle)
                .setSubtitle(mediaText.subtitle)
                .setArtist(artist)
                .setArtworkUri(artworkUrl?.toUri())
                .setDescription(qualityLabel)
                .setExtras(toExtras())
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build(),
        )
        .build()
}

internal data class TrackMediaText(
    val title: String,
    val albumTitle: String?,
    val subtitle: String?,
)

internal fun Track.mediaText(): TrackMediaText {
    val pageDisplayTitle = pageTitle
        ?.substringAfter(PAGE_TITLE_SEPARATOR, missingDelimiterValue = pageTitle)
        ?.trim()
        ?.takeIf(String::isNotBlank)
    val albumTitle = pageDisplayTitle?.let { pageName ->
        val exactPageSuffix = "$PAGE_TITLE_SEPARATOR$pageName"
        val titleWithoutPage = if (title.endsWith(exactPageSuffix)) {
            title.removeSuffix(exactPageSuffix)
        } else {
            // long: B 站可能只返回 P 序号而没有分 P 名称，此时按最后一个分隔符去掉仓库生成的“第 N P”兜底名称。
            title.substringBeforeLast(PAGE_TITLE_SEPARATOR, missingDelimiterValue = title)
        }
        titleWithoutPage.trim().ifBlank { title }
    }

    // long: 锁屏媒体卡片把 title 当作主标题；多 P 必须把当前分 P 名称放这里，同时用 albumTitle 保留视频总标题供 App 和历史继续展示。
    return TrackMediaText(
        title = pageDisplayTitle ?: title,
        albumTitle = albumTitle,
        subtitle = pageTitle,
    )
}

fun MediaItem.bilibiliSource(): BilibiliTrackSource? {
    val extras = mediaMetadata.extras ?: return null
    val bvid = extras.getString(EXTRA_BVID)?.takeIf(String::isNotBlank) ?: return null
    val cid = extras.getLong(EXTRA_CID, 0L).takeIf { it > 0L } ?: return null
    val qualityPreference = extras.getString(EXTRA_QUALITY_PREFERENCE)
        ?.let { value -> runCatching { AudioQualityPreference.valueOf(value) }.getOrNull() }
        ?: AudioQualityPreference.HIGHEST
    val aid = extras.getLong(EXTRA_AID, 0L).takeIf { it > 0L }
    return BilibiliTrackSource(bvid, cid, qualityPreference, aid)
}

fun MediaItem.toTrackOrNull(): Track? {
    val normalizedMediaId = mediaId.takeIf(String::isNotBlank) ?: return null
    val normalizedStreamUrl = localConfiguration?.uri?.toString()?.takeIf(String::isNotBlank) ?: return null
    val pageTitle = mediaMetadata.subtitle?.toString()?.takeIf(String::isNotBlank)
    val albumTitle = mediaMetadata.albumTitle?.toString()?.takeIf(String::isNotBlank)
    val fallbackTitle = mediaMetadata.title?.toString().orEmpty().ifBlank { normalizedMediaId }
    val resourceTitle = mediaMetadata.extras?.getString(EXTRA_RESOURCE_TITLE)
        ?.takeIf(String::isNotBlank)
        ?: if (albumTitle != null && pageTitle != null) {
            "$albumTitle$PAGE_TITLE_SEPARATOR${pageTitle.substringAfter(PAGE_TITLE_SEPARATOR, pageTitle)}"
        } else {
            albumTitle ?: fallbackTitle
        }
    return Track(
        id = normalizedMediaId,
        title = resourceTitle,
        artist = mediaMetadata.artist?.toString().orEmpty(),
        streamUrl = normalizedStreamUrl,
        artworkUrl = mediaMetadata.artworkUri?.toString(),
        qualityLabel = mediaMetadata.description?.toString()?.takeIf(String::isNotBlank),
        pageTitle = pageTitle,
        source = bilibiliSource(),
    )
}

private fun Track.toExtras(): Bundle {
    return Bundle().apply {
        // long: MediaSession 只保留展示标题会丢失多 P 的完整资源名，额外字段用于 Room 队列恢复后重建同一 Track。
        putString(EXTRA_RESOURCE_TITLE, title)
        source?.let { bilibiliSource ->
            putString(EXTRA_BVID, bilibiliSource.bvid)
            putLong(EXTRA_CID, bilibiliSource.cid)
            bilibiliSource.aid?.takeIf { it > 0L }?.let { putLong(EXTRA_AID, it) }
            putString(EXTRA_QUALITY_PREFERENCE, bilibiliSource.qualityPreference.name)
        }
    }
}

private const val EXTRA_BVID = "biu.bilibili.bvid"
private const val EXTRA_CID = "biu.bilibili.cid"
private const val EXTRA_AID = "biu.bilibili.aid"
private const val EXTRA_QUALITY_PREFERENCE = "biu.bilibili.quality_preference"
private const val EXTRA_RESOURCE_TITLE = "biu.playback.resource_title"
private const val PAGE_TITLE_SEPARATOR = " · "

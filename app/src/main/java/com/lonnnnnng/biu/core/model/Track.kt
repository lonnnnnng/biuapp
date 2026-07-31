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

fun MediaItem.playbackMediaMode(): PlaybackMediaMode {
    return PlaybackMediaMode.fromStoredValue(mediaMetadata.extras?.getString(EXTRA_PLAYBACK_MEDIA_MODE))
}

fun MediaItem.playbackStreamMetadata(): PlaybackStreamMetadata? {
    val extras = mediaMetadata.extras
    val localUrl = localConfiguration?.uri?.toString()?.takeIf(String::isNotBlank) ?: return null
    return if (playbackMediaMode() == PlaybackMediaMode.VIDEO) {
        val audioUrl = extras?.getString(EXTRA_AUDIO_STREAM_URL)?.takeIf(String::isNotBlank) ?: return null
        val videoUrl = extras.getString(EXTRA_VIDEO_STREAM_URL)?.takeIf(String::isNotBlank) ?: localUrl
        PlaybackStreamMetadata.video(
            audioUrl = audioUrl,
            videoUrl = videoUrl,
            audioQualityLabel = extras.getString(EXTRA_AUDIO_QUALITY_LABEL)?.takeIf(String::isNotBlank),
            videoQualityLabel = extras.getString(EXTRA_VIDEO_QUALITY_LABEL)?.takeIf(String::isNotBlank),
            selectedVideoQualityId = extras.getInt(EXTRA_SELECTED_VIDEO_QUALITY_ID, 0).takeIf { it > 0 },
            availableVideoQualities = playbackVideoQualities(extras),
        )
    } else {
        PlaybackStreamMetadata.audio(
            audioUrl = extras?.getString(EXTRA_AUDIO_STREAM_URL)?.takeIf(String::isNotBlank) ?: localUrl,
            qualityLabel = extras?.getString(EXTRA_AUDIO_QUALITY_LABEL)?.takeIf(String::isNotBlank)
                ?: mediaMetadata.description?.toString()?.takeIf(String::isNotBlank),
        )
    }
}

fun MediaItem.withAudioPlaybackStream(audioUrl: String, qualityLabel: String?): MediaItem {
    return withPlaybackStreamMetadata(PlaybackStreamMetadata.audio(audioUrl, qualityLabel))
}

fun MediaItem.withVideoPlaybackStreams(
    audioUrl: String,
    videoUrl: String,
    audioQualityLabel: String?,
    videoQualityLabel: String?,
    selectedVideoQualityId: Int? = null,
    availableVideoQualities: List<PlaybackVideoQuality> = emptyList(),
): MediaItem {
    return withPlaybackStreamMetadata(
        PlaybackStreamMetadata.video(
            audioUrl = audioUrl,
            videoUrl = videoUrl,
            audioQualityLabel = audioQualityLabel,
            videoQualityLabel = videoQualityLabel,
            selectedVideoQualityId = selectedVideoQualityId,
            availableVideoQualities = availableVideoQualities,
        ),
    )
}

fun MediaItem.toTrackOrNull(): Track? {
    val normalizedMediaId = mediaId.takeIf(String::isNotBlank) ?: return null
    // long: 内存中的视频项以视频轨作为主 URI；队列持久化必须改存音频轨，冷启动才能恢复产品默认的音频播放。
    val streamMetadata = playbackStreamMetadata()
    val normalizedStreamUrl = streamMetadata?.persistentAudioUrl
        ?: localConfiguration?.uri?.toString()?.takeIf(String::isNotBlank)
        ?: return null
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
        qualityLabel = streamMetadata?.audioQualityLabel
            ?: mediaMetadata.description?.toString()?.takeIf(String::isNotBlank),
        pageTitle = pageTitle,
        source = bilibiliSource(),
    )
}

private fun Track.toExtras(): Bundle {
    return Bundle().apply {
        // long: MediaSession 只保留展示标题会丢失多 P 的完整资源名，额外字段用于 Room 队列恢复后重建同一 Track。
        putString(EXTRA_RESOURCE_TITLE, title)
        putString(EXTRA_PLAYBACK_MEDIA_MODE, PlaybackMediaMode.AUDIO.name)
        putString(EXTRA_AUDIO_STREAM_URL, streamUrl)
        qualityLabel?.takeIf(String::isNotBlank)?.let { putString(EXTRA_AUDIO_QUALITY_LABEL, it) }
        source?.let { bilibiliSource ->
            putString(EXTRA_BVID, bilibiliSource.bvid)
            putLong(EXTRA_CID, bilibiliSource.cid)
            bilibiliSource.aid?.takeIf { it > 0L }?.let { putLong(EXTRA_AID, it) }
            putString(EXTRA_QUALITY_PREFERENCE, bilibiliSource.qualityPreference.name)
        }
    }
}

private fun MediaItem.withPlaybackStreamMetadata(stream: PlaybackStreamMetadata): MediaItem {
    val extras = Bundle(mediaMetadata.extras ?: Bundle()).apply {
        putString(EXTRA_PLAYBACK_MEDIA_MODE, stream.mode.name)
        putString(EXTRA_AUDIO_STREAM_URL, stream.audioUrl)
        putString(EXTRA_VIDEO_STREAM_URL, stream.videoUrl)
        putString(EXTRA_AUDIO_QUALITY_LABEL, stream.audioQualityLabel)
        putString(EXTRA_VIDEO_QUALITY_LABEL, stream.videoQualityLabel)
        if (stream.mode == PlaybackMediaMode.VIDEO) {
            stream.selectedVideoQualityId?.let { putInt(EXTRA_SELECTED_VIDEO_QUALITY_ID, it) }
                ?: remove(EXTRA_SELECTED_VIDEO_QUALITY_ID)
            putIntArray(
                EXTRA_AVAILABLE_VIDEO_QUALITY_IDS,
                stream.availableVideoQualities.map(PlaybackVideoQuality::qualityId).toIntArray(),
            )
            putStringArray(
                EXTRA_AVAILABLE_VIDEO_QUALITY_LABELS,
                stream.availableVideoQualities.map(PlaybackVideoQuality::label).toTypedArray(),
            )
        } else {
            // long: 视频画质只属于当前分 P 的内存播放状态；切回音频必须清理，队列持久化后不会误恢复旧视频模式。
            remove(EXTRA_SELECTED_VIDEO_QUALITY_ID)
            remove(EXTRA_AVAILABLE_VIDEO_QUALITY_IDS)
            remove(EXTRA_AVAILABLE_VIDEO_QUALITY_LABELS)
        }
    }
    val metadata = mediaMetadata.buildUpon()
        .setDescription(stream.displayQualityLabel)
        .setExtras(extras)
        .build()
    return buildUpon()
        .setUri(stream.playbackUrl.toUri())
        .setMediaMetadata(metadata)
        .build()
}

private fun playbackVideoQualities(extras: Bundle): List<PlaybackVideoQuality> {
    val ids = extras.getIntArray(EXTRA_AVAILABLE_VIDEO_QUALITY_IDS) ?: intArrayOf()
    val labels = extras.getStringArray(EXTRA_AVAILABLE_VIDEO_QUALITY_LABELS).orEmpty()
    return ids.indices.mapNotNull { index ->
        val id = ids[index]
        val label = labels.getOrNull(index)?.takeIf(String::isNotBlank)
        if (id > 0 && label != null) PlaybackVideoQuality(id, label) else null
    }
}

private const val EXTRA_BVID = "biu.bilibili.bvid"
private const val EXTRA_CID = "biu.bilibili.cid"
private const val EXTRA_AID = "biu.bilibili.aid"
private const val EXTRA_QUALITY_PREFERENCE = "biu.bilibili.quality_preference"
private const val EXTRA_RESOURCE_TITLE = "biu.playback.resource_title"
private const val EXTRA_PLAYBACK_MEDIA_MODE = "biu.playback.media_mode"
private const val EXTRA_AUDIO_STREAM_URL = "biu.playback.audio_stream_url"
private const val EXTRA_VIDEO_STREAM_URL = "biu.playback.video_stream_url"
private const val EXTRA_AUDIO_QUALITY_LABEL = "biu.playback.audio_quality_label"
private const val EXTRA_VIDEO_QUALITY_LABEL = "biu.playback.video_quality_label"
private const val EXTRA_SELECTED_VIDEO_QUALITY_ID = "biu.playback.selected_video_quality_id"
private const val EXTRA_AVAILABLE_VIDEO_QUALITY_IDS = "biu.playback.available_video_quality_ids"
private const val EXTRA_AVAILABLE_VIDEO_QUALITY_LABELS = "biu.playback.available_video_quality_labels"
private const val PAGE_TITLE_SEPARATOR = " · "

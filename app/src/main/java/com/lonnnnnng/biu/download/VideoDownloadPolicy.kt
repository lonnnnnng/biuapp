package com.lonnnnnng.biu.download

import androidx.media3.common.MediaItem
import com.lonnnnnng.biu.core.model.AudioQualityPreference
import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import com.lonnnnnng.biu.core.model.bilibiliSource

enum class VideoDownloadStatus {
    QUEUED,
    RESOLVING,
    DOWNLOADING_VIDEO,
    DOWNLOADING_AUDIO,
    MUXING,
    PUBLISHING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED,
}

object VideoDownloadStatePolicy {
    private val recoverableTargets = setOf(
        VideoDownloadStatus.PAUSED,
        VideoDownloadStatus.FAILED,
        VideoDownloadStatus.CANCELLED,
    )
    private val transitions = mapOf(
        VideoDownloadStatus.QUEUED to setOf(
            VideoDownloadStatus.RESOLVING,
            VideoDownloadStatus.PAUSED,
            VideoDownloadStatus.CANCELLED,
        ),
        VideoDownloadStatus.RESOLVING to recoverableTargets + VideoDownloadStatus.DOWNLOADING_VIDEO,
        VideoDownloadStatus.DOWNLOADING_VIDEO to recoverableTargets + VideoDownloadStatus.DOWNLOADING_AUDIO,
        VideoDownloadStatus.DOWNLOADING_AUDIO to recoverableTargets + VideoDownloadStatus.MUXING,
        VideoDownloadStatus.MUXING to recoverableTargets + VideoDownloadStatus.PUBLISHING,
        VideoDownloadStatus.PUBLISHING to recoverableTargets + VideoDownloadStatus.COMPLETED,
        VideoDownloadStatus.PAUSED to setOf(VideoDownloadStatus.QUEUED, VideoDownloadStatus.CANCELLED),
        VideoDownloadStatus.COMPLETED to emptySet(),
        VideoDownloadStatus.FAILED to setOf(VideoDownloadStatus.QUEUED, VideoDownloadStatus.CANCELLED),
        VideoDownloadStatus.CANCELLED to setOf(VideoDownloadStatus.QUEUED),
    )

    fun allowedTargets(status: VideoDownloadStatus): Set<VideoDownloadStatus> {
        return transitions.getValue(status)
    }

    fun canTransition(from: VideoDownloadStatus, to: VideoDownloadStatus): Boolean {
        return to in allowedTargets(from)
    }
}

data class VideoDownloadPublishSpec(
    val displayName: String,
    val mimeType: String,
    val relativePath: String?,
    val title: String,
    val isPending: Boolean,
)

object VideoDownloadPublishPolicy {
    fun spec(request: VideoDownloadRequest, sdkInt: Int): VideoDownloadPublishSpec {
        val supportsScopedMediaStore = sdkInt >= ANDROID_10_API_LEVEL
        return VideoDownloadPublishSpec(
            displayName = request.displayName,
            mimeType = VIDEO_MP4_MIME_TYPE,
            relativePath = if (supportsScopedMediaStore) BIU_MOVIES_RELATIVE_PATH else null,
            title = request.title,
            isPending = supportsScopedMediaStore,
        )
    }
}

data class VideoDownloadRequest(
    val taskId: String,
    val bvid: String,
    val cid: Long,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val qualityPreference: AudioQualityPreference,
) {
    val displayName: String
        get() = DownloadFileNamePolicy.displayName(
            title = title,
            artist = artist,
            titleFallback = "未知视频",
            artistFallback = "未知作者",
            extension = "mp4",
        )

    companion object {
        fun create(
            source: BilibiliTrackSource,
            currentTitle: String,
            resourceTitle: String,
            artist: String,
            artworkUrl: String?,
        ): VideoDownloadRequest {
            val normalizedTitle = currentTitle.trim()
                .ifBlank { resourceTitle.trim() }
                .ifBlank { source.bvid }
            val normalizedArtist = artist.trim().ifBlank { "未知作者" }
            return VideoDownloadRequest(
                taskId = "${source.bvid}:${source.cid}",
                bvid = source.bvid,
                cid = source.cid,
                title = normalizedTitle,
                artist = normalizedArtist,
                artworkUrl = artworkUrl,
                qualityPreference = source.qualityPreference,
            )
        }

        fun fromMediaItem(mediaItem: MediaItem): VideoDownloadRequest? {
            val source = mediaItem.bilibiliSource() ?: return null
            // long: 多 P 的 Media3 title 已是当前 P 名称，视频下载必须按当前 P 单独命名，albumTitle 只作为单 P/缺失标题的回退。
            return create(
                source = source,
                currentTitle = mediaItem.mediaMetadata.title?.toString().orEmpty(),
                resourceTitle = mediaItem.mediaMetadata.albumTitle?.toString().orEmpty(),
                artist = mediaItem.mediaMetadata.artist?.toString().orEmpty(),
                artworkUrl = mediaItem.mediaMetadata.artworkUri?.toString(),
            )
        }
    }
}

private const val ANDROID_10_API_LEVEL = 29
private const val VIDEO_MP4_MIME_TYPE = "video/mp4"
private const val BIU_MOVIES_RELATIVE_PATH = "Movies/Biu/"

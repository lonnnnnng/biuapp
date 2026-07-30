package com.lonnnnnng.biu.download

import androidx.media3.common.MediaItem
import com.lonnnnnng.biu.core.model.AudioQualityPreference
import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import com.lonnnnnng.biu.core.model.bilibiliSource

enum class AudioDownloadStatus {
    QUEUED,
    RESOLVING,
    DOWNLOADING,
    PAUSED,
    PUBLISHING,
    COMPLETED,
    FAILED,
    CANCELLED,
}

object AudioDownloadStatePolicy {
    private val transitions = mapOf(
        AudioDownloadStatus.QUEUED to setOf(
            AudioDownloadStatus.RESOLVING,
            AudioDownloadStatus.PAUSED,
            AudioDownloadStatus.CANCELLED,
        ),
        AudioDownloadStatus.RESOLVING to setOf(
            AudioDownloadStatus.DOWNLOADING,
            AudioDownloadStatus.PAUSED,
            AudioDownloadStatus.FAILED,
            AudioDownloadStatus.CANCELLED,
        ),
        AudioDownloadStatus.DOWNLOADING to setOf(
            AudioDownloadStatus.PAUSED,
            AudioDownloadStatus.PUBLISHING,
            AudioDownloadStatus.FAILED,
            AudioDownloadStatus.CANCELLED,
        ),
        AudioDownloadStatus.PAUSED to setOf(
            AudioDownloadStatus.QUEUED,
            AudioDownloadStatus.CANCELLED,
        ),
        AudioDownloadStatus.PUBLISHING to setOf(
            AudioDownloadStatus.PAUSED,
            AudioDownloadStatus.COMPLETED,
            AudioDownloadStatus.FAILED,
            AudioDownloadStatus.CANCELLED,
        ),
        AudioDownloadStatus.COMPLETED to emptySet(),
        AudioDownloadStatus.FAILED to setOf(
            AudioDownloadStatus.QUEUED,
            AudioDownloadStatus.CANCELLED,
        ),
        AudioDownloadStatus.CANCELLED to setOf(AudioDownloadStatus.QUEUED),
    )

    fun allowedTargets(status: AudioDownloadStatus): Set<AudioDownloadStatus> {
        return transitions.getValue(status)
    }

    fun canTransition(from: AudioDownloadStatus, to: AudioDownloadStatus): Boolean {
        return to in allowedTargets(from)
    }
}

data class AudioDownloadPublishSpec(
    val displayName: String,
    val mimeType: String,
    val relativePath: String?,
    val title: String,
    val artist: String,
    val isPending: Boolean,
)

object AudioDownloadPublishPolicy {
    fun spec(request: AudioDownloadRequest, sdkInt: Int): AudioDownloadPublishSpec {
        val supportsScopedMediaStore = sdkInt >= ANDROID_10_API_LEVEL
        return AudioDownloadPublishSpec(
            displayName = request.displayName,
            mimeType = AUDIO_MP4_MIME_TYPE,
            relativePath = if (supportsScopedMediaStore) BIU_MUSIC_RELATIVE_PATH else null,
            title = request.title,
            artist = request.artist,
            isPending = supportsScopedMediaStore,
        )
    }
}

data class AudioDownloadRequest(
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
            titleFallback = "未知曲目",
            artistFallback = "未知艺术家",
            extension = "m4a",
        )

    companion object {
        fun create(
            source: BilibiliTrackSource,
            currentTitle: String,
            resourceTitle: String,
            artist: String,
            artworkUrl: String?,
        ): AudioDownloadRequest {
            val normalizedTitle = currentTitle.trim()
                .ifBlank { resourceTitle.trim() }
                .ifBlank { source.bvid }
            val normalizedArtist = artist.trim().ifBlank { "未知艺术家" }
            return AudioDownloadRequest(
                taskId = "${source.bvid}:${source.cid}",
                bvid = source.bvid,
                cid = source.cid,
                title = normalizedTitle,
                artist = normalizedArtist,
                artworkUrl = artworkUrl,
                qualityPreference = source.qualityPreference,
            )
        }

        fun fromMediaItem(mediaItem: MediaItem): AudioDownloadRequest? {
            val source = mediaItem.bilibiliSource() ?: return null
            // long: Media3 的 title 在多 P 播放时保存当前 P 名称，albumTitle 才是视频总标题；下载必须跟随当前曲目而不是误用合集名称。
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
private const val AUDIO_MP4_MIME_TYPE = "audio/mp4"
private const val BIU_MUSIC_RELATIVE_PATH = "Music/Biu/"

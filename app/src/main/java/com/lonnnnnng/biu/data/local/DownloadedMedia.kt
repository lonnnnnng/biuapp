package com.lonnnnnng.biu.data.local

import com.lonnnnnng.biu.core.model.PlaybackMediaMode
import com.lonnnnnng.biu.core.model.Track
import com.lonnnnnng.biu.download.AudioDownloadStatus
import com.lonnnnnng.biu.download.VideoDownloadStatus

data class DownloadedMediaCopy(
    val uri: String,
    val mode: PlaybackMediaMode,
)

object DownloadedMediaPolicy {
    fun find(
        bvid: String,
        cid: Long,
        mode: PlaybackMediaMode,
        audioTasks: List<AudioDownloadTaskEntity>,
        videoTasks: List<VideoDownloadTaskEntity>,
    ): DownloadedMediaCopy? {
        val audio = audioTasks.firstOrNull { task ->
            task.bvid == bvid && task.cid == cid &&
                task.downloadStatus == AudioDownloadStatus.COMPLETED && !task.publishedUri.isNullOrBlank()
        }
        val video = videoTasks.firstOrNull { task ->
            task.bvid == bvid && task.cid == cid &&
                task.downloadStatus == VideoDownloadStatus.COMPLETED && !task.publishedUri.isNullOrBlank()
        }
        return when (mode) {
            PlaybackMediaMode.AUDIO -> audio?.let { DownloadedMediaCopy(requireNotNull(it.publishedUri), mode) }
                ?: video?.let { DownloadedMediaCopy(requireNotNull(it.publishedUri), PlaybackMediaMode.VIDEO) }
            PlaybackMediaMode.VIDEO -> video?.let { DownloadedMediaCopy(requireNotNull(it.publishedUri), mode) }
        }
    }

    fun preferLocal(track: Track, audioTasks: List<AudioDownloadTaskEntity>, videoTasks: List<VideoDownloadTaskEntity>): Track {
        val source = track.source ?: return track
        val copy = find(source.bvid, source.cid, PlaybackMediaMode.AUDIO, audioTasks, videoTasks) ?: return track
        return track.copy(
            streamUrl = copy.uri,
            qualityLabel = if (copy.mode == PlaybackMediaMode.AUDIO) "已下载" else "已下载视频",
            mimeType = if (copy.mode == PlaybackMediaMode.VIDEO) "video/mp4" else "audio/mp4",
        )
    }
}

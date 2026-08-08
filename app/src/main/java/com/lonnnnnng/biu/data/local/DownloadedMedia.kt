package com.lonnnnnng.biu.data.local

import com.lonnnnnng.biu.core.model.PlaybackMediaMode
import com.lonnnnnng.biu.core.model.Track
import com.lonnnnnng.biu.download.AudioDownloadStatus
import com.lonnnnnng.biu.download.VideoDownloadStatus

data class DownloadedMediaCopy(
    val uri: String,
    val mode: PlaybackMediaMode,
)

enum class DownloadedMediaStatus(val label: String) {
    AUDIO("音频已下载"),
    VIDEO("视频已下载"),
    AUDIO_AND_VIDEO("音视频已下载"),
}

data class DownloadedMediaKey(
    val bvid: String,
    val cid: Long,
)

class DownloadedMediaIndex internal constructor(
    private val byTrack: Map<DownloadedMediaKey, DownloadedMediaStatus>,
    private val byVideo: Map<String, DownloadedMediaStatus>,
) {
    fun status(bvid: String, cid: Long): DownloadedMediaStatus? {
        return byTrack[DownloadedMediaKey(bvid, cid)]
    }

    fun status(bvid: String): DownloadedMediaStatus? = byVideo[bvid]

    companion object {
        val EMPTY = DownloadedMediaIndex(emptyMap(), emptyMap())
    }
}

object DownloadedMediaPolicy {
    fun index(
        audioTasks: List<AudioDownloadTaskEntity>,
        videoTasks: List<VideoDownloadTaskEntity>,
    ): DownloadedMediaIndex {
        val byTrack = linkedMapOf<DownloadedMediaKey, DownloadedMediaStatus>()
        val byVideo = linkedMapOf<String, DownloadedMediaStatus>()

        // long: 只有已经发布到 MediaStore 的成品才算可离线使用；完成状态但 URI 缺失时不能向用户显示误导性的下载标记。
        audioTasks.asSequence()
            .filter(::isCompletedAudio)
            .forEach { task ->
                val key = DownloadedMediaKey(task.bvid, task.cid)
                byTrack[key] = mergeStatus(byTrack[key], DownloadedMediaStatus.AUDIO)
                byVideo[task.bvid] = mergeStatus(byVideo[task.bvid], DownloadedMediaStatus.AUDIO)
            }
        videoTasks.asSequence()
            .filter(::isCompletedVideo)
            .forEach { task ->
                val key = DownloadedMediaKey(task.bvid, task.cid)
                byTrack[key] = mergeStatus(byTrack[key], DownloadedMediaStatus.VIDEO)
                byVideo[task.bvid] = mergeStatus(byVideo[task.bvid], DownloadedMediaStatus.VIDEO)
            }
        return DownloadedMediaIndex(byTrack, byVideo)
    }

    fun find(
        bvid: String,
        cid: Long,
        mode: PlaybackMediaMode,
        audioTasks: List<AudioDownloadTaskEntity>,
        videoTasks: List<VideoDownloadTaskEntity>,
    ): DownloadedMediaCopy? {
        val audio = audioTasks.firstOrNull { task ->
            task.bvid == bvid && task.cid == cid && isCompletedAudio(task)
        }
        val video = videoTasks.firstOrNull { task ->
            task.bvid == bvid && task.cid == cid && isCompletedVideo(task)
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

    private fun isCompletedAudio(task: AudioDownloadTaskEntity): Boolean {
        return task.downloadStatus == AudioDownloadStatus.COMPLETED && !task.publishedUri.isNullOrBlank()
    }

    private fun isCompletedVideo(task: VideoDownloadTaskEntity): Boolean {
        return task.downloadStatus == VideoDownloadStatus.COMPLETED && !task.publishedUri.isNullOrBlank()
    }

    private fun mergeStatus(
        current: DownloadedMediaStatus?,
        incoming: DownloadedMediaStatus,
    ): DownloadedMediaStatus {
        if (current == null || current == incoming) return incoming
        return DownloadedMediaStatus.AUDIO_AND_VIDEO
    }
}

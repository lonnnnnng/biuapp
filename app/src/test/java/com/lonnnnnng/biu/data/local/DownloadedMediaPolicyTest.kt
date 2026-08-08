package com.lonnnnnng.biu.data.local

import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import com.lonnnnnng.biu.core.model.PlaybackMediaMode
import com.lonnnnnng.biu.core.model.Track
import com.lonnnnnng.biu.download.AudioDownloadStatus
import com.lonnnnnng.biu.download.VideoDownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadedMediaPolicyTest {
    @Test
    fun `听歌模式优先音频成品并按bvid和cid精确关联`() {
        val audio = audioTask("BV1", 11L, AudioDownloadStatus.COMPLETED, "content://audio/11")
        val video = videoTask("BV1", 11L, VideoDownloadStatus.COMPLETED, "content://video/11")

        val copy = DownloadedMediaPolicy.find(
            bvid = "BV1",
            cid = 11L,
            mode = PlaybackMediaMode.AUDIO,
            audioTasks = listOf(audio),
            videoTasks = listOf(video),
        )

        assertEquals("content://audio/11", copy?.uri)
        assertEquals(PlaybackMediaMode.AUDIO, copy?.mode)
        assertNull(
            DownloadedMediaPolicy.find(
                "BV1",
                12L,
                PlaybackMediaMode.AUDIO,
                listOf(audio),
                listOf(video),
            ),
        )
    }

    @Test
    fun `只有视频成品时听歌模式仍可使用其本地音轨`() {
        val track = Track(
            id = "BV1:11",
            title = "歌",
            artist = "UP",
            streamUrl = "https://cdn.example/audio.m4s",
            source = BilibiliTrackSource("BV1", 11L),
        )
        val video = videoTask("BV1", 11L, VideoDownloadStatus.COMPLETED, "content://video/11")

        val local = DownloadedMediaPolicy.preferLocal(track, emptyList(), listOf(video))

        assertEquals("content://video/11", local.streamUrl)
        assertEquals("已下载视频", local.qualityLabel)
        assertEquals("video/mp4", local.mimeType)
    }

    @Test
    fun `同一分P同时存在音频和视频成品时显示双份下载状态`() {
        val audio = audioTask("BV1", 11L, AudioDownloadStatus.COMPLETED, "content://audio/11")
        val video = videoTask("BV1", 11L, VideoDownloadStatus.COMPLETED, "content://video/11")

        val index = DownloadedMediaPolicy.index(listOf(audio), listOf(video))

        assertEquals(DownloadedMediaStatus.AUDIO_AND_VIDEO, index.status("BV1", 11L))
        assertEquals(DownloadedMediaStatus.AUDIO_AND_VIDEO, index.status("BV1"))
    }

    @Test
    fun `下载索引按分P精确区分并按主视频聚合`() {
        val audioP1 = audioTask("BV1", 11L, AudioDownloadStatus.COMPLETED, "content://audio/11")
        val videoP2 = videoTask("BV1", 12L, VideoDownloadStatus.COMPLETED, "content://video/12")

        val index = DownloadedMediaPolicy.index(listOf(audioP1), listOf(videoP2))

        assertEquals(DownloadedMediaStatus.AUDIO, index.status("BV1", 11L))
        assertEquals(DownloadedMediaStatus.VIDEO, index.status("BV1", 12L))
        assertNull(index.status("BV1", 13L))
        assertEquals(DownloadedMediaStatus.AUDIO_AND_VIDEO, index.status("BV1"))
    }

    @Test
    fun `下载索引忽略未完成任务和缺少MediaStore地址的伪成品`() {
        val downloading = audioTask("BV1", 11L, AudioDownloadStatus.DOWNLOADING, "content://audio/11")
        val missingUri = videoTask("BV1", 11L, VideoDownloadStatus.COMPLETED, null)

        val index = DownloadedMediaPolicy.index(listOf(downloading), listOf(missingUri))

        assertNull(index.status("BV1", 11L))
        assertNull(index.status("BV1"))
    }

    private fun audioTask(
        bvid: String,
        cid: Long,
        status: AudioDownloadStatus,
        uri: String?,
    ) = AudioDownloadTaskEntity(
        taskId = "$bvid:$cid", bvid = bvid, cid = cid, title = "歌", artist = "UP", artworkUrl = null,
        qualityPreference = "HIGHEST", displayName = "歌.m4a", status = status.name, downloadedBytes = 1L,
        totalBytes = 1L, tempFilePath = "", qualityLabel = "", publishedUri = uri, errorMessage = null,
        createdAtEpochMs = 1L, updatedAtEpochMs = 1L,
    )

    private fun videoTask(
        bvid: String,
        cid: Long,
        status: VideoDownloadStatus,
        uri: String?,
    ) = VideoDownloadTaskEntity(
        taskId = "$bvid:$cid", bvid = bvid, cid = cid, title = "歌", artist = "UP", artworkUrl = null,
        qualityPreference = "HIGHEST", displayName = "歌.mp4", status = status.name,
        videoDownloadedBytes = 1L, videoTotalBytes = 1L, audioDownloadedBytes = 1L, audioTotalBytes = 1L,
        videoTempFilePath = "", audioTempFilePath = "", outputTempFilePath = "", videoQualityLabel = "",
        audioQualityLabel = "", outputBytes = 2L, publishedUri = uri, errorMessage = null,
        createdAtEpochMs = 1L, updatedAtEpochMs = 1L,
    )
}

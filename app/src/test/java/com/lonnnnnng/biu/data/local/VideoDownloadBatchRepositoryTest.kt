package com.lonnnnnng.biu.data.local

import com.lonnnnnng.biu.core.model.AudioQualityPreference
import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import com.lonnnnnng.biu.download.VideoDownloadRequest
import com.lonnnnnng.biu.download.VideoDownloadStatus
import com.lonnnnnng.biu.download.VideoDownloadTempFiles
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoDownloadBatchRepositoryTest {
    @Test
    fun `批量入队跳过已完成和运行任务并保留失败任务双轨断点`() = runBlocking {
        val dao = FakeVideoDownloadTaskDao(
            listOf(
                entity(1L, VideoDownloadStatus.COMPLETED),
                entity(
                    cid = 2L,
                    status = VideoDownloadStatus.FAILED,
                    videoDownloadedBytes = 512L,
                    audioDownloadedBytes = 128L,
                    videoPath = "saved.video.m4s",
                    audioPath = "saved.audio.m4s",
                ),
                entity(3L, VideoDownloadStatus.DOWNLOADING_AUDIO),
            ),
        )
        val repository = VideoDownloadRepository(dao, nowEpochMs = { 2_000L })

        val result = repository.enqueueAll(
            requests = (1L..4L).map(::request),
            tempFiles = { taskId ->
                VideoDownloadTempFiles(
                    video = File("/tmp/$taskId.video.m4s"),
                    audio = File("/tmp/$taskId.audio.m4s"),
                    output = File("/tmp/$taskId.output.mp4"),
                )
            },
        )

        assertEquals(4, result.requestedCount)
        assertEquals(2, result.queuedCount)
        assertEquals(1, result.skippedCompletedCount)
        assertEquals(1, result.skippedExistingCount)
        assertEquals(VideoDownloadStatus.QUEUED, dao.find("BV1VIDEO:2")?.downloadStatus)
        assertEquals(512L, dao.find("BV1VIDEO:2")?.videoDownloadedBytes)
        assertEquals(128L, dao.find("BV1VIDEO:2")?.audioDownloadedBytes)
        assertEquals("saved.video.m4s", dao.find("BV1VIDEO:2")?.videoTempFilePath)
        assertEquals("saved.audio.m4s", dao.find("BV1VIDEO:2")?.audioTempFilePath)
        assertEquals(VideoDownloadStatus.QUEUED, dao.find("BV1VIDEO:4")?.downloadStatus)
    }

    @Test
    fun `一键重试只把失败视频任务重新排队`() = runBlocking {
        val dao = FakeVideoDownloadTaskDao(
            listOf(
                entity(1L, VideoDownloadStatus.FAILED),
                entity(2L, VideoDownloadStatus.PAUSED),
            ),
        )
        val repository = VideoDownloadRepository(dao, nowEpochMs = { 3_000L })

        val count = repository.retryFailedTasks()

        assertEquals(1, count)
        assertEquals(VideoDownloadStatus.QUEUED, dao.find("BV1VIDEO:1")?.downloadStatus)
        assertEquals(VideoDownloadStatus.PAUSED, dao.find("BV1VIDEO:2")?.downloadStatus)
    }

    private fun request(cid: Long): VideoDownloadRequest {
        return VideoDownloadRequest.create(
            source = BilibiliTrackSource("BV1VIDEO", cid, AudioQualityPreference.HIGHEST),
            currentTitle = "P$cid",
            resourceTitle = "合集",
            artist = "作者",
            artworkUrl = null,
        )
    }

    private fun entity(
        cid: Long,
        status: VideoDownloadStatus,
        videoDownloadedBytes: Long = 0L,
        audioDownloadedBytes: Long = 0L,
        videoPath: String = "/tmp/$cid.video.m4s",
        audioPath: String = "/tmp/$cid.audio.m4s",
    ) = VideoDownloadTaskEntity(
        taskId = "BV1VIDEO:$cid",
        bvid = "BV1VIDEO",
        cid = cid,
        title = "P$cid",
        artist = "作者",
        artworkUrl = null,
        qualityPreference = AudioQualityPreference.HIGHEST.name,
        displayName = "P$cid.mp4",
        status = status.name,
        videoDownloadedBytes = videoDownloadedBytes,
        videoTotalBytes = if (videoDownloadedBytes > 0L) 2_048L else 0L,
        audioDownloadedBytes = audioDownloadedBytes,
        audioTotalBytes = if (audioDownloadedBytes > 0L) 1_024L else 0L,
        videoTempFilePath = videoPath,
        audioTempFilePath = audioPath,
        outputTempFilePath = "/tmp/$cid.output.mp4",
        videoQualityLabel = "1080p · AVC",
        audioQualityLabel = "192 kbps",
        outputBytes = 0L,
        publishedUri = if (status == VideoDownloadStatus.COMPLETED) "content://video/$cid" else null,
        errorMessage = if (status == VideoDownloadStatus.FAILED) "失败" else null,
        createdAtEpochMs = cid,
        updatedAtEpochMs = cid,
    )

    private class FakeVideoDownloadTaskDao(initial: List<VideoDownloadTaskEntity>) : VideoDownloadTaskDao {
        private val entities = initial.associateByTo(linkedMapOf(), VideoDownloadTaskEntity::taskId)
        private val flow = MutableStateFlow(snapshot())

        override fun observeAll(): Flow<List<VideoDownloadTaskEntity>> = flow

        override suspend fun find(taskId: String): VideoDownloadTaskEntity? = entities[taskId]

        override suspend fun findAll(taskIds: List<String>): List<VideoDownloadTaskEntity> {
            return taskIds.mapNotNull(entities::get)
        }

        override suspend fun nextQueued(): VideoDownloadTaskEntity? {
            return entities.values.filter { it.downloadStatus == VideoDownloadStatus.QUEUED }
                .minByOrNull(VideoDownloadTaskEntity::createdAtEpochMs)
        }

        override suspend fun upsert(entity: VideoDownloadTaskEntity) {
            entities[entity.taskId] = entity
            publish()
        }

        override suspend fun upsertAll(entities: List<VideoDownloadTaskEntity>) {
            entities.forEach { entity -> this.entities[entity.taskId] = entity }
            publish()
        }

        override suspend fun updateStatus(
            taskId: String,
            status: String,
            videoQualityLabel: String,
            audioQualityLabel: String,
            errorMessage: String?,
            updatedAtEpochMs: Long,
        ) {
            entities[taskId]?.let { current ->
                entities[taskId] = current.copy(
                    status = status,
                    videoQualityLabel = videoQualityLabel,
                    audioQualityLabel = audioQualityLabel,
                    errorMessage = errorMessage,
                    updatedAtEpochMs = updatedAtEpochMs,
                )
            }
            publish()
        }

        override suspend fun updateVideoProgress(
            taskId: String,
            downloadedBytes: Long,
            totalBytes: Long,
            updatedAtEpochMs: Long,
        ) {
            entities[taskId]?.let { current ->
                entities[taskId] = current.copy(
                    videoDownloadedBytes = downloadedBytes,
                    videoTotalBytes = totalBytes,
                    updatedAtEpochMs = updatedAtEpochMs,
                )
            }
            publish()
        }

        override suspend fun updateAudioProgress(
            taskId: String,
            downloadedBytes: Long,
            totalBytes: Long,
            updatedAtEpochMs: Long,
        ) {
            entities[taskId]?.let { current ->
                entities[taskId] = current.copy(
                    audioDownloadedBytes = downloadedBytes,
                    audioTotalBytes = totalBytes,
                    updatedAtEpochMs = updatedAtEpochMs,
                )
            }
            publish()
        }

        override suspend fun markCompleted(
            taskId: String,
            completedStatus: String,
            outputBytes: Long,
            publishedUri: String,
            updatedAtEpochMs: Long,
        ) {
            entities[taskId]?.let { current ->
                entities[taskId] = current.copy(
                    status = completedStatus,
                    outputBytes = outputBytes,
                    publishedUri = publishedUri,
                    errorMessage = null,
                    updatedAtEpochMs = updatedAtEpochMs,
                )
            }
            publish()
        }

        override suspend fun pauseInterrupted(
            runningStatuses: List<String>,
            pausedStatus: String,
            updatedAtEpochMs: Long,
        ) {
            entities.replaceAll { _, current ->
                if (current.status in runningStatuses) {
                    current.copy(status = pausedStatus, errorMessage = null, updatedAtEpochMs = updatedAtEpochMs)
                } else {
                    current
                }
            }
            publish()
        }

        override suspend fun retryFailed(failedStatus: String, queuedStatus: String, updatedAtEpochMs: Long): Int {
            var count = 0
            entities.replaceAll { _, current ->
                if (current.status == failedStatus) {
                    count += 1
                    current.copy(status = queuedStatus, errorMessage = null, updatedAtEpochMs = updatedAtEpochMs)
                } else {
                    current
                }
            }
            publish()
            return count
        }

        private fun publish() {
            flow.value = snapshot()
        }

        private fun snapshot(): List<VideoDownloadTaskEntity> {
            return entities.values.sortedByDescending(VideoDownloadTaskEntity::createdAtEpochMs)
        }
    }
}

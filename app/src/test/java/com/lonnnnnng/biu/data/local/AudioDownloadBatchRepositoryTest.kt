package com.lonnnnnng.biu.data.local

import com.lonnnnnng.biu.core.model.AudioQualityPreference
import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import com.lonnnnnng.biu.download.AudioDownloadRequest
import com.lonnnnnng.biu.download.AudioDownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioDownloadBatchRepositoryTest {
    @Test
    fun `批量入队跳过已完成和运行任务并保留失败任务断点`() = runBlocking {
        val dao = FakeAudioDownloadTaskDao(
            listOf(
                entity(1L, AudioDownloadStatus.COMPLETED),
                entity(2L, AudioDownloadStatus.FAILED, downloadedBytes = 512L, totalBytes = 2048L, tempPath = "saved.part"),
                entity(3L, AudioDownloadStatus.DOWNLOADING),
            ),
        )
        val repository = AudioDownloadRepository(dao, nowEpochMs = { 2_000L })

        val result = repository.enqueueAll(
            requests = (1L..4L).map(::request),
            tempFilePath = { taskId -> "/tmp/$taskId.part" },
        )

        assertEquals(4, result.requestedCount)
        assertEquals(2, result.queuedCount)
        assertEquals(1, result.skippedCompletedCount)
        assertEquals(1, result.skippedExistingCount)
        assertEquals(AudioDownloadStatus.QUEUED, dao.find("BV1BATCH:2")?.downloadStatus)
        assertEquals(512L, dao.find("BV1BATCH:2")?.downloadedBytes)
        assertEquals("saved.part", dao.find("BV1BATCH:2")?.tempFilePath)
        assertEquals(AudioDownloadStatus.QUEUED, dao.find("BV1BATCH:4")?.downloadStatus)
    }

    @Test
    fun `一键重试只把失败任务重新排队`() = runBlocking {
        val dao = FakeAudioDownloadTaskDao(
            listOf(
                entity(1L, AudioDownloadStatus.FAILED),
                entity(2L, AudioDownloadStatus.PAUSED),
            ),
        )
        val repository = AudioDownloadRepository(dao, nowEpochMs = { 3_000L })

        val count = repository.retryFailedTasks()

        assertEquals(1, count)
        assertEquals(AudioDownloadStatus.QUEUED, dao.find("BV1BATCH:1")?.downloadStatus)
        assertEquals(AudioDownloadStatus.PAUSED, dao.find("BV1BATCH:2")?.downloadStatus)
    }

    private fun request(cid: Long): AudioDownloadRequest {
        return AudioDownloadRequest.create(
            source = BilibiliTrackSource("BV1BATCH", cid, AudioQualityPreference.HIGHEST),
            currentTitle = "P$cid",
            resourceTitle = "合集",
            artist = "作者",
            artworkUrl = null,
        )
    }

    private fun entity(
        cid: Long,
        status: AudioDownloadStatus,
        downloadedBytes: Long = 0L,
        totalBytes: Long = 0L,
        tempPath: String = "/tmp/$cid.part",
    ) = AudioDownloadTaskEntity(
        taskId = "BV1BATCH:$cid",
        bvid = "BV1BATCH",
        cid = cid,
        title = "P$cid",
        artist = "作者",
        artworkUrl = null,
        qualityPreference = AudioQualityPreference.HIGHEST.name,
        displayName = "P$cid.m4a",
        status = status.name,
        downloadedBytes = downloadedBytes,
        totalBytes = totalBytes,
        tempFilePath = tempPath,
        qualityLabel = "",
        publishedUri = if (status == AudioDownloadStatus.COMPLETED) "content://audio/$cid" else null,
        errorMessage = if (status == AudioDownloadStatus.FAILED) "失败" else null,
        createdAtEpochMs = cid,
        updatedAtEpochMs = cid,
    )

    private class FakeAudioDownloadTaskDao(initial: List<AudioDownloadTaskEntity>) : AudioDownloadTaskDao {
        private val entities = initial.associateByTo(linkedMapOf(), AudioDownloadTaskEntity::taskId)
        private val flow = MutableStateFlow(snapshot())

        override fun observeAll(): Flow<List<AudioDownloadTaskEntity>> = flow

        override suspend fun find(taskId: String): AudioDownloadTaskEntity? = entities[taskId]

        override suspend fun findAll(taskIds: List<String>): List<AudioDownloadTaskEntity> {
            return taskIds.mapNotNull(entities::get)
        }

        override suspend fun nextQueued(): AudioDownloadTaskEntity? {
            return entities.values.filter { it.downloadStatus == AudioDownloadStatus.QUEUED }
                .minByOrNull(AudioDownloadTaskEntity::createdAtEpochMs)
        }

        override suspend fun upsert(entity: AudioDownloadTaskEntity) {
            entities[entity.taskId] = entity
            publish()
        }

        override suspend fun upsertAll(entities: List<AudioDownloadTaskEntity>) {
            entities.forEach { entity -> this.entities[entity.taskId] = entity }
            publish()
        }

        override suspend fun updateStatus(
            taskId: String,
            status: String,
            qualityLabel: String,
            errorMessage: String?,
            updatedAtEpochMs: Long,
        ) {
            entities[taskId]?.let { current ->
                entities[taskId] = current.copy(
                    status = status,
                    qualityLabel = qualityLabel,
                    errorMessage = errorMessage,
                    updatedAtEpochMs = updatedAtEpochMs,
                )
            }
            publish()
        }

        override suspend fun updateProgress(
            taskId: String,
            downloadedBytes: Long,
            totalBytes: Long,
            updatedAtEpochMs: Long,
        ) {
            entities[taskId]?.let { current ->
                entities[taskId] = current.copy(
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes,
                    updatedAtEpochMs = updatedAtEpochMs,
                )
            }
            publish()
        }

        override suspend fun markCompleted(
            taskId: String,
            completedStatus: String,
            downloadedBytes: Long,
            totalBytes: Long,
            publishedUri: String,
            updatedAtEpochMs: Long,
        ) {
            entities[taskId]?.let { current ->
                entities[taskId] = current.copy(
                    status = completedStatus,
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes,
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

        private fun snapshot(): List<AudioDownloadTaskEntity> {
            return entities.values.sortedByDescending(AudioDownloadTaskEntity::createdAtEpochMs)
        }
    }
}

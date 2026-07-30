package com.lonnnnnng.biu.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.lonnnnnng.biu.core.model.AudioQualityPreference
import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import com.lonnnnnng.biu.download.AudioDownloadRequest
import com.lonnnnnng.biu.download.AudioDownloadStatePolicy
import com.lonnnnnng.biu.download.AudioDownloadStatus
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "audio_download_tasks")
data class AudioDownloadTaskEntity(
    @PrimaryKey val taskId: String,
    val bvid: String,
    val cid: Long,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val qualityPreference: String,
    val displayName: String,
    val status: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val tempFilePath: String,
    val qualityLabel: String,
    val publishedUri: String?,
    val errorMessage: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
) {
    val downloadStatus: AudioDownloadStatus
        get() = runCatching { AudioDownloadStatus.valueOf(status) }
            .getOrDefault(AudioDownloadStatus.FAILED)

    val progressFraction: Float
        get() = if (totalBytes > 0L) {
            (downloadedBytes.toDouble() / totalBytes.toDouble()).coerceIn(0.0, 1.0).toFloat()
        } else {
            0f
        }

    fun toRequest(): AudioDownloadRequest {
        return AudioDownloadRequest.create(
            source = BilibiliTrackSource(
                bvid = bvid,
                cid = cid,
                qualityPreference = runCatching { AudioQualityPreference.valueOf(qualityPreference) }
                    .getOrDefault(AudioQualityPreference.HIGHEST),
            ),
            currentTitle = title,
            resourceTitle = title,
            artist = artist,
            artworkUrl = artworkUrl,
        )
    }
}

@Dao
interface AudioDownloadTaskDao {
    @Query("SELECT * FROM audio_download_tasks ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<AudioDownloadTaskEntity>>

    @Query("SELECT * FROM audio_download_tasks WHERE taskId = :taskId LIMIT 1")
    suspend fun find(taskId: String): AudioDownloadTaskEntity?

    @Query("SELECT * FROM audio_download_tasks WHERE status = 'QUEUED' ORDER BY createdAtEpochMs ASC LIMIT 1")
    suspend fun nextQueued(): AudioDownloadTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AudioDownloadTaskEntity)

    @Query(
        """
        UPDATE audio_download_tasks
        SET status = :status,
            qualityLabel = :qualityLabel,
            errorMessage = :errorMessage,
            updatedAtEpochMs = :updatedAtEpochMs
        WHERE taskId = :taskId
        """,
    )
    suspend fun updateStatus(
        taskId: String,
        status: String,
        qualityLabel: String,
        errorMessage: String?,
        updatedAtEpochMs: Long,
    )

    @Query(
        """
        UPDATE audio_download_tasks
        SET downloadedBytes = :downloadedBytes,
            totalBytes = :totalBytes,
            updatedAtEpochMs = :updatedAtEpochMs
        WHERE taskId = :taskId
        """,
    )
    suspend fun updateProgress(
        taskId: String,
        downloadedBytes: Long,
        totalBytes: Long,
        updatedAtEpochMs: Long,
    )

    @Query(
        """
        UPDATE audio_download_tasks
        SET status = :completedStatus,
            downloadedBytes = :downloadedBytes,
            totalBytes = :totalBytes,
            publishedUri = :publishedUri,
            errorMessage = NULL,
            updatedAtEpochMs = :updatedAtEpochMs
        WHERE taskId = :taskId
        """,
    )
    suspend fun markCompleted(
        taskId: String,
        completedStatus: String,
        downloadedBytes: Long,
        totalBytes: Long,
        publishedUri: String,
        updatedAtEpochMs: Long,
    )

    @Query(
        """
        UPDATE audio_download_tasks
        SET status = :pausedStatus,
            errorMessage = NULL,
            updatedAtEpochMs = :updatedAtEpochMs
        WHERE status IN (:runningStatuses)
        """,
    )
    suspend fun pauseInterrupted(
        runningStatuses: List<String>,
        pausedStatus: String,
        updatedAtEpochMs: Long,
    )
}

class AudioDownloadRepository(
    private val dao: AudioDownloadTaskDao,
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) {
    val tasks: Flow<List<AudioDownloadTaskEntity>> = dao.observeAll()

    suspend fun find(taskId: String): AudioDownloadTaskEntity? = dao.find(taskId)

    suspend fun nextQueued(): AudioDownloadTaskEntity? = dao.nextQueued()

    suspend fun enqueue(request: AudioDownloadRequest, tempFilePath: String): AudioDownloadTaskEntity {
        val now = nowEpochMs()
        val existing = dao.find(request.taskId)
        if (existing?.downloadStatus == AudioDownloadStatus.COMPLETED) return existing
        if (existing != null && existing.downloadStatus !in setOf(
                AudioDownloadStatus.PAUSED,
                AudioDownloadStatus.FAILED,
                AudioDownloadStatus.CANCELLED,
                AudioDownloadStatus.QUEUED,
            )
        ) {
            return existing
        }
        val preservePartialFile = existing?.downloadStatus in setOf(
            AudioDownloadStatus.PAUSED,
            AudioDownloadStatus.FAILED,
        )
        val entity = AudioDownloadTaskEntity(
            taskId = request.taskId,
            bvid = request.bvid,
            cid = request.cid,
            title = request.title,
            artist = request.artist,
            artworkUrl = request.artworkUrl,
            qualityPreference = request.qualityPreference.name,
            displayName = request.displayName,
            status = AudioDownloadStatus.QUEUED.name,
            downloadedBytes = if (preservePartialFile) existing?.downloadedBytes ?: 0L else 0L,
            totalBytes = if (preservePartialFile) existing?.totalBytes ?: 0L else 0L,
            tempFilePath = existing?.tempFilePath?.takeIf { preservePartialFile } ?: tempFilePath,
            qualityLabel = existing?.qualityLabel.orEmpty(),
            publishedUri = null,
            errorMessage = null,
            createdAtEpochMs = existing?.createdAtEpochMs ?: now,
            updatedAtEpochMs = now,
        )
        dao.upsert(entity)
        return entity
    }

    suspend fun transition(
        taskId: String,
        target: AudioDownloadStatus,
        qualityLabel: String? = null,
        errorMessage: String? = null,
    ): AudioDownloadTaskEntity? {
        val current = dao.find(taskId) ?: return null
        if (current.downloadStatus == target) return current
        check(AudioDownloadStatePolicy.canTransition(current.downloadStatus, target)) {
            "下载任务不能从 ${current.downloadStatus} 切换到 $target"
        }
        dao.updateStatus(
            taskId = taskId,
            status = target.name,
            qualityLabel = qualityLabel ?: current.qualityLabel,
            errorMessage = errorMessage,
            updatedAtEpochMs = nowEpochMs(),
        )
        return dao.find(taskId)
    }

    suspend fun updateProgress(taskId: String, downloadedBytes: Long, totalBytes: Long) {
        dao.updateProgress(
            taskId = taskId,
            downloadedBytes = downloadedBytes.coerceAtLeast(0L),
            totalBytes = totalBytes.coerceAtLeast(0L),
            updatedAtEpochMs = nowEpochMs(),
        )
    }

    suspend fun markCompleted(taskId: String, publishedUri: String, sizeBytes: Long) {
        val current = dao.find(taskId) ?: return
        check(AudioDownloadStatePolicy.canTransition(current.downloadStatus, AudioDownloadStatus.COMPLETED)) {
            "只有发布中的任务才能完成"
        }
        dao.markCompleted(
            taskId = taskId,
            completedStatus = AudioDownloadStatus.COMPLETED.name,
            downloadedBytes = sizeBytes.coerceAtLeast(0L),
            totalBytes = sizeBytes.coerceAtLeast(0L),
            publishedUri = publishedUri,
            updatedAtEpochMs = nowEpochMs(),
        )
    }

    suspend fun pauseInterruptedTasks() {
        // long: 进程死亡会同时终止网络流，冷启动必须把无法继续运行的中间态降级为暂停，避免 UI 永久显示“下载中”。
        dao.pauseInterrupted(
            runningStatuses = listOf(
                AudioDownloadStatus.RESOLVING.name,
                AudioDownloadStatus.DOWNLOADING.name,
                AudioDownloadStatus.PUBLISHING.name,
            ),
            pausedStatus = AudioDownloadStatus.PAUSED.name,
            updatedAtEpochMs = nowEpochMs(),
        )
    }
}

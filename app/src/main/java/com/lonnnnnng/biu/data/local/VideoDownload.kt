package com.lonnnnnng.biu.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.lonnnnnng.biu.core.model.AudioQualityPreference
import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import com.lonnnnnng.biu.download.VideoDownloadRequest
import com.lonnnnnng.biu.download.VideoDownloadStatePolicy
import com.lonnnnnng.biu.download.VideoDownloadStatus
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "video_download_tasks")
data class VideoDownloadTaskEntity(
    @PrimaryKey val taskId: String,
    val bvid: String,
    val cid: Long,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val qualityPreference: String,
    val displayName: String,
    val status: String,
    val videoDownloadedBytes: Long,
    val videoTotalBytes: Long,
    val audioDownloadedBytes: Long,
    val audioTotalBytes: Long,
    val videoTempFilePath: String,
    val audioTempFilePath: String,
    val outputTempFilePath: String,
    val videoQualityLabel: String,
    val audioQualityLabel: String,
    val outputBytes: Long,
    val publishedUri: String?,
    val errorMessage: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
) {
    val downloadStatus: VideoDownloadStatus
        get() = runCatching { VideoDownloadStatus.valueOf(status) }
            .getOrDefault(VideoDownloadStatus.FAILED)

    val progressFraction: Float
        get() {
            val downloaded = videoDownloadedBytes + audioDownloadedBytes
            val total = videoTotalBytes + audioTotalBytes
            return if (total > 0L) {
                (downloaded.toDouble() / total.toDouble()).coerceIn(0.0, 1.0).toFloat()
            } else {
                0f
            }
        }

    fun toRequest(): VideoDownloadRequest {
        return VideoDownloadRequest.create(
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
interface VideoDownloadTaskDao {
    @Query("SELECT * FROM video_download_tasks ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<VideoDownloadTaskEntity>>

    @Query("SELECT * FROM video_download_tasks WHERE taskId = :taskId LIMIT 1")
    suspend fun find(taskId: String): VideoDownloadTaskEntity?

    @Query("SELECT * FROM video_download_tasks WHERE status = 'QUEUED' ORDER BY createdAtEpochMs ASC LIMIT 1")
    suspend fun nextQueued(): VideoDownloadTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VideoDownloadTaskEntity)

    @Query(
        """
        UPDATE video_download_tasks
        SET status = :status,
            videoQualityLabel = :videoQualityLabel,
            audioQualityLabel = :audioQualityLabel,
            errorMessage = :errorMessage,
            updatedAtEpochMs = :updatedAtEpochMs
        WHERE taskId = :taskId
        """,
    )
    suspend fun updateStatus(
        taskId: String,
        status: String,
        videoQualityLabel: String,
        audioQualityLabel: String,
        errorMessage: String?,
        updatedAtEpochMs: Long,
    )

    @Query(
        """
        UPDATE video_download_tasks
        SET videoDownloadedBytes = :downloadedBytes,
            videoTotalBytes = :totalBytes,
            updatedAtEpochMs = :updatedAtEpochMs
        WHERE taskId = :taskId
        """,
    )
    suspend fun updateVideoProgress(taskId: String, downloadedBytes: Long, totalBytes: Long, updatedAtEpochMs: Long)

    @Query(
        """
        UPDATE video_download_tasks
        SET audioDownloadedBytes = :downloadedBytes,
            audioTotalBytes = :totalBytes,
            updatedAtEpochMs = :updatedAtEpochMs
        WHERE taskId = :taskId
        """,
    )
    suspend fun updateAudioProgress(taskId: String, downloadedBytes: Long, totalBytes: Long, updatedAtEpochMs: Long)

    @Query(
        """
        UPDATE video_download_tasks
        SET status = :completedStatus,
            outputBytes = :outputBytes,
            publishedUri = :publishedUri,
            errorMessage = NULL,
            updatedAtEpochMs = :updatedAtEpochMs
        WHERE taskId = :taskId
        """,
    )
    suspend fun markCompleted(
        taskId: String,
        completedStatus: String,
        outputBytes: Long,
        publishedUri: String,
        updatedAtEpochMs: Long,
    )

    @Query(
        """
        UPDATE video_download_tasks
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

class VideoDownloadRepository(
    private val dao: VideoDownloadTaskDao,
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) {
    val tasks: Flow<List<VideoDownloadTaskEntity>> = dao.observeAll()

    suspend fun find(taskId: String): VideoDownloadTaskEntity? = dao.find(taskId)

    suspend fun nextQueued(): VideoDownloadTaskEntity? = dao.nextQueued()

    suspend fun enqueue(
        request: VideoDownloadRequest,
        videoTempFilePath: String,
        audioTempFilePath: String,
        outputTempFilePath: String,
    ): VideoDownloadTaskEntity {
        val now = nowEpochMs()
        val existing = dao.find(request.taskId)
        if (existing?.downloadStatus == VideoDownloadStatus.COMPLETED) return existing
        if (existing != null && existing.downloadStatus !in setOf(
                VideoDownloadStatus.PAUSED,
                VideoDownloadStatus.FAILED,
                VideoDownloadStatus.CANCELLED,
                VideoDownloadStatus.QUEUED,
            )
        ) {
            return existing
        }
        val preservePartialFiles = existing?.downloadStatus in setOf(
            VideoDownloadStatus.PAUSED,
            VideoDownloadStatus.FAILED,
        )
        val entity = VideoDownloadTaskEntity(
            taskId = request.taskId,
            bvid = request.bvid,
            cid = request.cid,
            title = request.title,
            artist = request.artist,
            artworkUrl = request.artworkUrl,
            qualityPreference = request.qualityPreference.name,
            displayName = request.displayName,
            status = VideoDownloadStatus.QUEUED.name,
            videoDownloadedBytes = if (preservePartialFiles) existing?.videoDownloadedBytes ?: 0L else 0L,
            videoTotalBytes = if (preservePartialFiles) existing?.videoTotalBytes ?: 0L else 0L,
            audioDownloadedBytes = if (preservePartialFiles) existing?.audioDownloadedBytes ?: 0L else 0L,
            audioTotalBytes = if (preservePartialFiles) existing?.audioTotalBytes ?: 0L else 0L,
            videoTempFilePath = existing?.videoTempFilePath?.takeIf { preservePartialFiles } ?: videoTempFilePath,
            audioTempFilePath = existing?.audioTempFilePath?.takeIf { preservePartialFiles } ?: audioTempFilePath,
            outputTempFilePath = existing?.outputTempFilePath?.takeIf { preservePartialFiles } ?: outputTempFilePath,
            videoQualityLabel = existing?.videoQualityLabel.orEmpty(),
            audioQualityLabel = existing?.audioQualityLabel.orEmpty(),
            outputBytes = 0L,
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
        target: VideoDownloadStatus,
        videoQualityLabel: String? = null,
        audioQualityLabel: String? = null,
        errorMessage: String? = null,
    ): VideoDownloadTaskEntity? {
        val current = dao.find(taskId) ?: return null
        if (current.downloadStatus == target) return current
        check(VideoDownloadStatePolicy.canTransition(current.downloadStatus, target)) {
            "视频下载任务不能从 ${current.downloadStatus} 切换到 $target"
        }
        dao.updateStatus(
            taskId = taskId,
            status = target.name,
            videoQualityLabel = videoQualityLabel ?: current.videoQualityLabel,
            audioQualityLabel = audioQualityLabel ?: current.audioQualityLabel,
            errorMessage = errorMessage,
            updatedAtEpochMs = nowEpochMs(),
        )
        return dao.find(taskId)
    }

    suspend fun updateVideoProgress(taskId: String, downloadedBytes: Long, totalBytes: Long) {
        dao.updateVideoProgress(taskId, downloadedBytes.coerceAtLeast(0L), totalBytes.coerceAtLeast(0L), nowEpochMs())
    }

    suspend fun updateAudioProgress(taskId: String, downloadedBytes: Long, totalBytes: Long) {
        dao.updateAudioProgress(taskId, downloadedBytes.coerceAtLeast(0L), totalBytes.coerceAtLeast(0L), nowEpochMs())
    }

    suspend fun markCompleted(taskId: String, publishedUri: String, outputBytes: Long) {
        val current = dao.find(taskId) ?: return
        check(VideoDownloadStatePolicy.canTransition(current.downloadStatus, VideoDownloadStatus.COMPLETED)) {
            "只有发布中的视频任务才能完成"
        }
        dao.markCompleted(
            taskId = taskId,
            completedStatus = VideoDownloadStatus.COMPLETED.name,
            outputBytes = outputBytes.coerceAtLeast(0L),
            publishedUri = publishedUri,
            updatedAtEpochMs = nowEpochMs(),
        )
    }

    suspend fun pauseInterruptedTasks() {
        // long: 双轨下载、合并或发布在进程死亡后都不会继续执行，冷启动统一降级为暂停，保留已下载轨文件等待用户恢复。
        dao.pauseInterrupted(
            runningStatuses = listOf(
                VideoDownloadStatus.RESOLVING.name,
                VideoDownloadStatus.DOWNLOADING_VIDEO.name,
                VideoDownloadStatus.DOWNLOADING_AUDIO.name,
                VideoDownloadStatus.MUXING.name,
                VideoDownloadStatus.PUBLISHING.name,
            ),
            pausedStatus = VideoDownloadStatus.PAUSED.name,
            updatedAtEpochMs = nowEpochMs(),
        )
    }
}

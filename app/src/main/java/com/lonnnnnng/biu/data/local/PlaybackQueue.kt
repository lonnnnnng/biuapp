package com.lonnnnnng.biu.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import com.lonnnnnng.biu.core.model.AudioQualityPreference
import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import com.lonnnnnng.biu.core.model.Track

@Entity(tableName = "playback_queue_state")
data class PlaybackQueueStateEntity(
    @PrimaryKey val singletonId: Int = PLAYBACK_QUEUE_SINGLETON_ID,
    val currentIndex: Int,
    val currentPositionMs: Long,
    val updatedAtEpochMs: Long,
)

@Entity(tableName = "playback_queue_items")
data class PlaybackQueueItemEntity(
    @PrimaryKey val position: Int,
    val mediaId: String,
    val title: String,
    val artist: String,
    val streamUrl: String,
    val artworkUrl: String?,
    val qualityLabel: String?,
    val pageTitle: String?,
    val bvid: String?,
    val cid: Long?,
    val qualityPreference: String?,
)

@Dao
interface PlaybackQueueDao {
    @Query("SELECT * FROM playback_queue_state WHERE singletonId = $PLAYBACK_QUEUE_SINGLETON_ID LIMIT 1")
    suspend fun currentState(): PlaybackQueueStateEntity?

    @Query("SELECT * FROM playback_queue_items ORDER BY position ASC")
    suspend fun currentItems(): List<PlaybackQueueItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertState(state: PlaybackQueueStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PlaybackQueueItemEntity>)

    @Query("DELETE FROM playback_queue_state")
    suspend fun deleteState()

    @Query("DELETE FROM playback_queue_items")
    suspend fun deleteItems()

    @Transaction
    suspend fun replace(state: PlaybackQueueStateEntity, items: List<PlaybackQueueItemEntity>) {
        // long: 应用只保留当前活动队列；同一事务先清旧项再写新快照，避免进程中断后状态与媒体项来自两代队列。
        deleteItems()
        insertItems(items)
        upsertState(state)
    }

    @Transaction
    suspend fun clear() {
        deleteItems()
        deleteState()
    }
}

data class PlaybackQueueRecord(
    val items: List<Track>,
    val currentIndex: Int,
    val currentPositionMs: Long,
) {
    init {
        require(items.isNotEmpty()) { "播放队列不能为空" }
        require(currentIndex in items.indices) { "当前播放索引越界" }
    }
}

class PlaybackQueueRepository(
    private val dao: PlaybackQueueDao,
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) {
    suspend fun replace(queue: PlaybackQueueRecord) {
        dao.replace(
            state = PlaybackQueueStateEntity(
                currentIndex = queue.currentIndex,
                currentPositionMs = queue.currentPositionMs.coerceAtLeast(0L),
                updatedAtEpochMs = nowEpochMs(),
            ),
            items = queue.items.mapIndexed { index, track -> track.toEntity(index) },
        )
    }

    suspend fun load(): PlaybackQueueRecord? {
        val state = dao.currentState() ?: return null
        val entities = dao.currentItems()
        if (entities.isEmpty()) return null
        val tracks = entities.map { entity -> entity.toTrackOrNull() ?: return null }
        return PlaybackQueueRecord(
            items = tracks,
            // long: Room 数据若因旧测试包或异常写入留下越界索引，恢复到最近有效项，不能让冷启动直接崩溃。
            currentIndex = state.currentIndex.coerceIn(tracks.indices),
            currentPositionMs = state.currentPositionMs.coerceAtLeast(0L),
        )
    }

    suspend fun clear() = dao.clear()
}

private fun Track.toEntity(position: Int): PlaybackQueueItemEntity {
    return PlaybackQueueItemEntity(
        position = position,
        mediaId = id,
        title = title,
        artist = artist,
        // long: 当前 URI 作为快速恢复缓存保留；B 站地址失效时仍以 bvid/cid 为准走播放服务的重新解析链路。
        streamUrl = streamUrl,
        artworkUrl = artworkUrl,
        qualityLabel = qualityLabel,
        pageTitle = pageTitle,
        bvid = source?.bvid,
        cid = source?.cid,
        qualityPreference = source?.qualityPreference?.name,
    )
}

private fun PlaybackQueueItemEntity.toTrackOrNull(): Track? {
    val normalizedMediaId = mediaId.takeIf(String::isNotBlank) ?: return null
    val normalizedStreamUrl = streamUrl.takeIf(String::isNotBlank) ?: return null
    val source = bvid?.takeIf(String::isNotBlank)?.let { normalizedBvid ->
        val normalizedCid = cid?.takeIf { it > 0L } ?: return null
        BilibiliTrackSource(
            bvid = normalizedBvid,
            cid = normalizedCid,
            qualityPreference = qualityPreference
                ?.let { value -> runCatching { AudioQualityPreference.valueOf(value) }.getOrNull() }
                ?: AudioQualityPreference.HIGHEST,
        )
    }
    return Track(
        id = normalizedMediaId,
        title = title,
        artist = artist,
        streamUrl = normalizedStreamUrl,
        artworkUrl = artworkUrl,
        qualityLabel = qualityLabel,
        pageTitle = pageTitle,
        source = source,
    )
}

private const val PLAYBACK_QUEUE_SINGLETON_ID = 1

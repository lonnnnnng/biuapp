package com.lonnnnnng.biu.data.local

import androidx.media3.common.MediaItem
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lonnnnnng.biu.core.model.AudioQualityPreference
import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import com.lonnnnnng.biu.core.model.bilibiliSource
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey val mediaId: String,
    val bvid: String,
    val cid: Long,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val qualityPreference: String,
    val lastPositionMs: Long,
    val durationMs: Long,
    val playedAtEpochMs: Long,
    val playCount: Int,
) {
    val source: BilibiliTrackSource
        get() = BilibiliTrackSource(
            bvid = bvid,
            cid = cid,
            qualityPreference = runCatching { AudioQualityPreference.valueOf(qualityPreference) }
                .getOrDefault(AudioQualityPreference.HIGHEST),
        )
}

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY playedAtEpochMs DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE mediaId = :mediaId LIMIT 1")
    suspend fun find(mediaId: String): PlaybackHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlaybackHistoryEntity)

    @Query("UPDATE playback_history SET lastPositionMs = :positionMs, durationMs = :durationMs WHERE mediaId = :mediaId")
    suspend fun updateProgress(mediaId: String, positionMs: Long, durationMs: Long)

    @Query("DELETE FROM playback_history")
    suspend fun clear()
}

@Database(
    entities = [PlaybackHistoryEntity::class, CreatorSelectionEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class BiuDatabase : RoomDatabase() {
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun creatorSelectionDao(): CreatorSelectionDao
}

object BiuDatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // long: 升级只新增首页 UP 配置表，已有播放历史必须原样保留，不能使用破坏性迁移。
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `creator_selection` (
                    `mid` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `faceUrl` TEXT NOT NULL,
                    `position` INTEGER NOT NULL,
                    PRIMARY KEY(`mid`)
                )
                """.trimIndent(),
            )
        }
    }
}

class PlaybackHistoryRepository(
    private val dao: PlaybackHistoryDao,
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) {
    val recent: Flow<List<PlaybackHistoryEntity>> = dao.observeRecent(100)

    suspend fun recordStarted(mediaItem: MediaItem) {
        val source = mediaItem.bilibiliSource() ?: return
        val mediaId = mediaItem.mediaId.takeIf(String::isNotBlank) ?: "${source.bvid}:${source.cid}"
        val existing = dao.find(mediaId)
        // long: DASH 地址会过期，历史只保存可重新解析的 bvid/cid 与展示元数据，避免重启后播放失效链接。
        dao.upsert(
            PlaybackHistoryEntity(
                mediaId = mediaId,
                bvid = source.bvid,
                cid = source.cid,
                // long: 多 P 的媒体 title 会随分 P 切换，历史继续保存 albumTitle 中的视频总标题，避免恢复播放后资源名称退化为单个分 P 名称。
                title = mediaItem.mediaMetadata.albumTitle?.toString().orEmpty()
                    .ifBlank { mediaItem.mediaMetadata.title?.toString().orEmpty() }
                    .ifBlank { source.bvid },
                artist = mediaItem.mediaMetadata.artist?.toString().orEmpty(),
                artworkUrl = mediaItem.mediaMetadata.artworkUri?.toString(),
                qualityPreference = source.qualityPreference.name,
                lastPositionMs = existing?.lastPositionMs ?: 0L,
                durationMs = existing?.durationMs ?: 0L,
                playedAtEpochMs = nowEpochMs(),
                playCount = (existing?.playCount ?: 0) + 1,
            ),
        )
    }

    suspend fun recordProgress(mediaItem: MediaItem?, positionMs: Long, durationMs: Long) {
        val mediaId = mediaItem?.mediaId?.takeIf(String::isNotBlank) ?: return
        dao.updateProgress(
            mediaId = mediaId,
            positionMs = positionMs.coerceAtLeast(0L),
            durationMs = durationMs.coerceAtLeast(0L),
        )
    }

    suspend fun clear() = dao.clear()
}

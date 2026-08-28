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
            // long: 数据库字段继续保留以兼容旧表，历史恢复时不再沿用已下线的省流量档。
            qualityPreference = AudioQualityPreference.HIGHEST,
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
    entities = [
        PlaybackHistoryEntity::class,
        CreatorSelectionEntity::class,
        AudioDownloadTaskEntity::class,
        VideoDownloadTaskEntity::class,
        PlaybackQueueStateEntity::class,
        PlaybackQueueItemEntity::class,
        LyricsCacheEntity::class,
        CreatorGroupEntity::class,
        CreatorGroupMemberEntity::class,
        LocalPlaylistEntity::class,
        LocalPlaylistItemEntity::class,
    ],
    version = 10,
    exportSchema = false,
)
abstract class BiuDatabase : RoomDatabase() {
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun creatorSelectionDao(): CreatorSelectionDao
    abstract fun audioDownloadTaskDao(): AudioDownloadTaskDao
    abstract fun videoDownloadTaskDao(): VideoDownloadTaskDao
    abstract fun playbackQueueDao(): PlaybackQueueDao
    abstract fun lyricsCacheDao(): LyricsCacheDao
    abstract fun creatorGroupDao(): CreatorGroupDao
    abstract fun localPlaylistDao(): LocalPlaylistDao
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

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // long: 下载任务必须与播放历史共存；升级仅新增任务表，保留用户已有的账号配置、历史进度和播放次数。
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `audio_download_tasks` (
                    `taskId` TEXT NOT NULL,
                    `bvid` TEXT NOT NULL,
                    `cid` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `artist` TEXT NOT NULL,
                    `artworkUrl` TEXT,
                    `qualityPreference` TEXT NOT NULL,
                    `displayName` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `downloadedBytes` INTEGER NOT NULL,
                    `totalBytes` INTEGER NOT NULL,
                    `tempFilePath` TEXT NOT NULL,
                    `qualityLabel` TEXT NOT NULL,
                    `publishedUri` TEXT,
                    `errorMessage` TEXT,
                    `createdAtEpochMs` INTEGER NOT NULL,
                    `updatedAtEpochMs` INTEGER NOT NULL,
                    PRIMARY KEY(`taskId`)
                )
                """.trimIndent(),
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // long: M5 新增视频双轨任务表，不改写现有音频任务；升级后用户已完成的音频下载和历史进度必须原样保留。
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `video_download_tasks` (
                    `taskId` TEXT NOT NULL,
                    `bvid` TEXT NOT NULL,
                    `cid` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `artist` TEXT NOT NULL,
                    `artworkUrl` TEXT,
                    `qualityPreference` TEXT NOT NULL,
                    `displayName` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `videoDownloadedBytes` INTEGER NOT NULL,
                    `videoTotalBytes` INTEGER NOT NULL,
                    `audioDownloadedBytes` INTEGER NOT NULL,
                    `audioTotalBytes` INTEGER NOT NULL,
                    `videoTempFilePath` TEXT NOT NULL,
                    `audioTempFilePath` TEXT NOT NULL,
                    `outputTempFilePath` TEXT NOT NULL,
                    `videoQualityLabel` TEXT NOT NULL,
                    `audioQualityLabel` TEXT NOT NULL,
                    `outputBytes` INTEGER NOT NULL,
                    `publishedUri` TEXT,
                    `errorMessage` TEXT,
                    `createdAtEpochMs` INTEGER NOT NULL,
                    `updatedAtEpochMs` INTEGER NOT NULL,
                    PRIMARY KEY(`taskId`)
                )
                """.trimIndent(),
            )
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // long: 队列恢复只新增两张单例快照表，升级时不得改写已有历史、账号选择和音视频下载断点。
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `playback_queue_state` (
                    `singletonId` INTEGER NOT NULL,
                    `currentIndex` INTEGER NOT NULL,
                    `currentPositionMs` INTEGER NOT NULL,
                    `updatedAtEpochMs` INTEGER NOT NULL,
                    PRIMARY KEY(`singletonId`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `playback_queue_items` (
                    `position` INTEGER NOT NULL,
                    `mediaId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `artist` TEXT NOT NULL,
                    `streamUrl` TEXT NOT NULL,
                    `artworkUrl` TEXT,
                    `qualityLabel` TEXT,
                    `pageTitle` TEXT,
                    `bvid` TEXT,
                    `cid` INTEGER,
                    `qualityPreference` TEXT,
                    PRIMARY KEY(`position`)
                )
                """.trimIndent(),
            )
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // long: 歌词缓存作为独立附加数据升级，既有播放队列、历史进度和下载断点全部原样保留。
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `lyrics_cache` (
                    `cacheKey` TEXT NOT NULL,
                    `source` TEXT NOT NULL,
                    `rawLyrics` TEXT NOT NULL,
                    `providerId` INTEGER,
                    `trackName` TEXT,
                    `artistName` TEXT,
                    `isUserSelected` INTEGER NOT NULL,
                    `updatedAtEpochMs` INTEGER NOT NULL,
                    PRIMARY KEY(`cacheKey`)
                )
                """.trimIndent(),
            )
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // long: M15 只新增本地 UP 主分组和成员关系，保留旧首页范围、历史、队列及下载断点。
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `creator_groups` (
                    `groupId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `position` INTEGER NOT NULL,
                    `createdAtEpochMs` INTEGER NOT NULL,
                    `updatedAtEpochMs` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `creator_group_members` (
                    `groupId` INTEGER NOT NULL,
                    `mid` INTEGER NOT NULL,
                    `position` INTEGER NOT NULL,
                    `addedAtEpochMs` INTEGER NOT NULL,
                    PRIMARY KEY(`groupId`, `mid`),
                    FOREIGN KEY(`groupId`) REFERENCES `creator_groups`(`groupId`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_creator_group_members_mid` ON `creator_group_members` (`mid`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_creator_group_members_groupId_position` ON `creator_group_members` (`groupId`, `position`)")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // long: 本地歌单只新增稳定曲目索引；B 站临时播放地址不进入表，升级不会污染现有队列与下载记录。
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `local_playlists` (
                    `playlistId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `position` INTEGER NOT NULL,
                    `createdAtEpochMs` INTEGER NOT NULL,
                    `updatedAtEpochMs` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `local_playlist_items` (
                    `playlistId` INTEGER NOT NULL,
                    `mediaId` TEXT NOT NULL,
                    `position` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `artist` TEXT NOT NULL,
                    `artworkUrl` TEXT,
                    `pageTitle` TEXT,
                    `streamUrl` TEXT,
                    `bvid` TEXT,
                    `cid` INTEGER,
                    `aid` INTEGER,
                    `publishedAtEpochSeconds` INTEGER,
                    `addedAtEpochMs` INTEGER NOT NULL,
                    PRIMARY KEY(`playlistId`, `mediaId`),
                    FOREIGN KEY(`playlistId`) REFERENCES `local_playlists`(`playlistId`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_local_playlist_items_playlistId_position` ON `local_playlist_items` (`playlistId`, `position`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_local_playlist_items_bvid_cid` ON `local_playlist_items` (`bvid`, `cid`)")
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // long: 歌词偏移属于每首曲目的校准结果；只给既有缓存追加默认零偏移，所有歌词选择、历史、歌单和下载记录都必须原样保留。
            db.execSQL("ALTER TABLE `lyrics_cache` ADD COLUMN `offsetMs` INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // long: 播放队列需要区分“进程异常退出前正在播放”和“用户主动暂停”，默认旧版本保持暂停，避免升级后误播。
            db.execSQL("ALTER TABLE `playback_queue_state` ADD COLUMN `shouldResumePlayback` INTEGER NOT NULL DEFAULT 0")
        }
    }
}

class PlaybackHistoryRepository(
    private val dao: PlaybackHistoryDao,
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) {
    val recent: Flow<List<PlaybackHistoryEntity>> = dao.observeRecent(100)

    suspend fun find(mediaId: String): PlaybackHistoryEntity? = dao.find(mediaId)

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

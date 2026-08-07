package com.lonnnnnng.biu.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import com.lonnnnnng.biu.core.model.AudioQualityPreference
import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import com.lonnnnnng.biu.core.model.Track
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "local_playlists")
data class LocalPlaylistEntity(
    @PrimaryKey(autoGenerate = true) val playlistId: Long = 0L,
    val name: String,
    val position: Int,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

/** long: 歌单条目以媒体稳定 ID 去重；B 站分 P 的 ID 为 bvid:cid，本地音频为 local:MediaStoreId。 */
@Entity(
    tableName = "local_playlist_items",
    primaryKeys = ["playlistId", "mediaId"],
    foreignKeys = [
        ForeignKey(
            entity = LocalPlaylistEntity::class,
            parentColumns = ["playlistId"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("playlistId", "position"), Index("bvid", "cid")],
)
data class LocalPlaylistItemEntity(
    val playlistId: Long,
    val mediaId: String,
    val position: Int,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val pageTitle: String?,
    val streamUrl: String?,
    val bvid: String?,
    val cid: Long?,
    val aid: Long?,
    val publishedAtEpochSeconds: Long?,
    val addedAtEpochMs: Long,
) {
    val isBilibili: Boolean
        get() = !bvid.isNullOrBlank() && cid != null && cid > 0L

    fun toStoredTrack(): Track = Track(
        id = mediaId,
        title = title,
        artist = artist,
        streamUrl = streamUrl.orEmpty(),
        artworkUrl = artworkUrl,
        pageTitle = pageTitle,
        publishedAtEpochSeconds = publishedAtEpochSeconds,
        source = if (isBilibili) {
            BilibiliTrackSource(
                bvid = requireNotNull(bvid),
                cid = requireNotNull(cid),
                qualityPreference = AudioQualityPreference.HIGHEST,
                aid = aid,
            )
        } else {
            null
        },
    )
}

@Dao
interface LocalPlaylistDao {
    @Query("SELECT * FROM local_playlists ORDER BY position ASC, playlistId ASC")
    fun observePlaylists(): Flow<List<LocalPlaylistEntity>>

    @Query("SELECT * FROM local_playlist_items WHERE playlistId = :playlistId ORDER BY position ASC, addedAtEpochMs ASC")
    fun observeItems(playlistId: Long): Flow<List<LocalPlaylistItemEntity>>

    @Insert
    suspend fun insertPlaylist(playlist: LocalPlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylist(playlist: LocalPlaylistEntity)

    @Query("DELETE FROM local_playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM local_playlist_items WHERE playlistId = :playlistId")
    suspend fun nextItemPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: LocalPlaylistItemEntity)

    @Query("DELETE FROM local_playlist_items WHERE playlistId = :playlistId AND mediaId = :mediaId")
    suspend fun deleteItem(playlistId: Long, mediaId: String)

    @Query("UPDATE local_playlist_items SET position = :position WHERE playlistId = :playlistId AND mediaId = :mediaId")
    suspend fun updateItemPosition(playlistId: Long, mediaId: String, position: Int)

    @Transaction
    suspend fun replaceItemOrder(playlistId: Long, mediaIds: List<String>) {
        mediaIds.forEachIndexed { index, mediaId -> updateItemPosition(playlistId, mediaId, index) }
    }
}

class LocalPlaylistRepository(
    private val dao: LocalPlaylistDao,
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) {
    val playlists: Flow<List<LocalPlaylistEntity>> = dao.observePlaylists()

    fun items(playlistId: Long): Flow<List<LocalPlaylistItemEntity>> = dao.observeItems(playlistId)

    suspend fun create(name: String, position: Int): Long {
        val normalized = name.trim()
        require(normalized.isNotEmpty()) { "歌单名称不能为空" }
        val now = nowEpochMs()
        return dao.insertPlaylist(
            LocalPlaylistEntity(
                name = normalized,
                position = position.coerceAtLeast(0),
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
        )
    }

    suspend fun rename(playlist: LocalPlaylistEntity, name: String) {
        val normalized = name.trim()
        require(normalized.isNotEmpty()) { "歌单名称不能为空" }
        dao.upsertPlaylist(playlist.copy(name = normalized, updatedAtEpochMs = nowEpochMs()))
    }

    suspend fun delete(playlistId: Long) = dao.deletePlaylist(playlistId)

    suspend fun addTrack(playlistId: Long, track: Track) {
        val source = track.source
        dao.upsertItem(
            LocalPlaylistItemEntity(
                playlistId = playlistId,
                mediaId = track.id,
                position = dao.nextItemPosition(playlistId),
                title = track.title,
                artist = track.artist,
                artworkUrl = track.artworkUrl,
                pageTitle = track.pageTitle,
                // long: B 站地址会过期，只把本地 content URI 持久化；在线曲目播放时按 bvid/cid 重新解析。
                streamUrl = track.streamUrl.takeIf { source == null && it.isNotBlank() },
                bvid = source?.bvid,
                cid = source?.cid,
                aid = source?.aid,
                publishedAtEpochSeconds = track.publishedAtEpochSeconds,
                addedAtEpochMs = nowEpochMs(),
            ),
        )
    }

    suspend fun remove(playlistId: Long, mediaId: String) = dao.deleteItem(playlistId, mediaId)

    suspend fun reorder(playlistId: Long, mediaIds: List<String>) {
        dao.replaceItemOrder(playlistId, mediaIds.distinct())
    }
}

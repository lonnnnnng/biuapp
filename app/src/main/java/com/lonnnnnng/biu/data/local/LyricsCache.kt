package com.lonnnnnng.biu.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.lonnnnnng.biu.data.lyrics.LrcParser
import com.lonnnnnng.biu.data.lyrics.LyricsDocument
import com.lonnnnnng.biu.data.lyrics.LyricsSource

@Entity(tableName = "lyrics_cache")
data class LyricsCacheEntity(
    @PrimaryKey val cacheKey: String,
    val source: String,
    val rawLyrics: String,
    val providerId: Long?,
    val trackName: String?,
    val artistName: String?,
    val isUserSelected: Boolean,
    val offsetMs: Long,
    val updatedAtEpochMs: Long,
)

@Dao
interface LyricsCacheDao {
    @Query("SELECT * FROM lyrics_cache WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun find(cacheKey: String): LyricsCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LyricsCacheEntity)

    @Query("DELETE FROM lyrics_cache WHERE cacheKey = :cacheKey")
    suspend fun delete(cacheKey: String)
}

class LyricsCacheRepository(
    private val dao: LyricsCacheDao,
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) {
    suspend fun find(cacheKey: String): LyricsDocument? {
        val entity = dao.find(cacheKey) ?: return null
        val lines = LrcParser.parse(entity.rawLyrics)
        if (lines.isEmpty()) {
            // long: 损坏或旧格式缓存不能永久挡住用户重新搜索；无法形成时间轴时立即淘汰，下一次由用户手动请求 LRCLIB。
            dao.delete(cacheKey)
            return null
        }
        val source = runCatching { LyricsSource.valueOf(entity.source) }.getOrNull()
        if (source == null) {
            // long: 数据源枚举已经废弃的缓存不能继续留在本地，读取时清除可避免每次点击歌词都重复命中无效记录。
            dao.delete(cacheKey)
            return null
        }
        return LyricsDocument(
            cacheKey = entity.cacheKey,
            source = source,
            lines = lines,
            rawLyrics = entity.rawLyrics,
            providerId = entity.providerId,
            trackName = entity.trackName,
            artistName = entity.artistName,
            isUserSelected = entity.isUserSelected,
            offsetMs = entity.offsetMs,
        )
    }

    suspend fun save(document: LyricsDocument) {
        require(document.cacheKey.isNotBlank()) { "歌词缓存键不能为空" }
        require(document.rawLyrics.isNotBlank()) { "歌词缓存内容不能为空" }
        // long: 同一 bvid/cid 只保留用户当前选中的 LRCLIB 版本，重新选择时原子覆盖，冷启动继续沿用用户决定。
        dao.upsert(
            LyricsCacheEntity(
                cacheKey = document.cacheKey,
                source = document.source.name,
                rawLyrics = document.rawLyrics,
                providerId = document.providerId,
                trackName = document.trackName,
                artistName = document.artistName,
                isUserSelected = document.isUserSelected,
                offsetMs = document.offsetMs,
                updatedAtEpochMs = nowEpochMs(),
            ),
        )
    }

    suspend fun delete(cacheKey: String) = dao.delete(cacheKey)
}

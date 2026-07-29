package com.lonnnnnng.biu.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import com.lonnnnnng.biu.data.bilibili.BilibiliCreator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "creator_selection")
data class CreatorSelectionEntity(
    @PrimaryKey val mid: Long,
    val name: String,
    val faceUrl: String,
    val position: Int,
) {
    fun toCreator() = BilibiliCreator(mid = mid, name = name, faceUrl = faceUrl)
}

@Dao
interface CreatorSelectionDao {
    @Query("SELECT * FROM creator_selection ORDER BY position ASC")
    fun observeAll(): Flow<List<CreatorSelectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CreatorSelectionEntity>)

    @Query("DELETE FROM creator_selection")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(entities: List<CreatorSelectionEntity>) {
        // long: 首页范围是一次完整配置，先清空再写入并置于同一事务，避免观察者读到半套选择结果。
        deleteAll()
        if (entities.isNotEmpty()) insertAll(entities)
    }
}

class CreatorSelectionRepository(private val dao: CreatorSelectionDao) {
    val selected: Flow<List<BilibiliCreator>> = dao.observeAll().map { entities ->
        entities.map(CreatorSelectionEntity::toCreator)
    }

    suspend fun replaceAll(creators: List<BilibiliCreator>) {
        val entities = creators.distinctBy(BilibiliCreator::mid).mapIndexed { index, creator ->
            CreatorSelectionEntity(
                mid = creator.mid,
                name = creator.name,
                faceUrl = creator.faceUrl,
                position = index,
            )
        }
        dao.replaceAll(entities)
    }
}

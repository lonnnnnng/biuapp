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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** long: UP 主分组独立于首页选择保存，避免用户改首页范围时丢失本地整理关系。 */
@Entity(tableName = "creator_groups")
data class CreatorGroupEntity(
    @PrimaryKey(autoGenerate = true) val groupId: Long = 0L,
    val name: String,
    val position: Int,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

/** long: 一个 UP 主可进入多个本地分组，复合主键保证重复添加不会产生重复条目。 */
@Entity(
    tableName = "creator_group_members",
    primaryKeys = ["groupId", "mid"],
    foreignKeys = [
        ForeignKey(
            entity = CreatorGroupEntity::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("mid"), Index("groupId", "position")],
)
data class CreatorGroupMemberEntity(
    val groupId: Long,
    val mid: Long,
    val position: Int,
    val addedAtEpochMs: Long,
)

@Dao
interface CreatorGroupDao {
    @Query("SELECT * FROM creator_groups ORDER BY position ASC, groupId ASC")
    fun observeGroups(): Flow<List<CreatorGroupEntity>>

    @Query("SELECT * FROM creator_group_members WHERE groupId = :groupId ORDER BY position ASC, mid ASC")
    fun observeMembers(groupId: Long): Flow<List<CreatorGroupMemberEntity>>

    @Query("SELECT * FROM creator_group_members ORDER BY groupId ASC, position ASC, mid ASC")
    fun observeAllMembers(): Flow<List<CreatorGroupMemberEntity>>

    @Insert
    suspend fun insertGroup(group: CreatorGroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroup(group: CreatorGroupEntity)

    @Query("DELETE FROM creator_groups WHERE groupId = :groupId")
    suspend fun deleteGroup(groupId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<CreatorGroupMemberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMember(member: CreatorGroupMemberEntity)

    @Query("DELETE FROM creator_group_members WHERE groupId = :groupId")
    suspend fun deleteMembers(groupId: Long)

    @Query("DELETE FROM creator_group_members WHERE groupId = :groupId AND mid = :mid")
    suspend fun deleteMember(groupId: Long, mid: Long)

    @Transaction
    suspend fun replaceMembers(groupId: Long, members: List<CreatorGroupMemberEntity>) {
        deleteMembers(groupId)
        if (members.isNotEmpty()) insertMembers(members)
    }
}

class CreatorGroupRepository(
    private val dao: CreatorGroupDao,
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) {
    val groups: Flow<List<CreatorGroupEntity>> = dao.observeGroups()
    val memberships: Flow<Map<Long, Set<Long>>> = dao.observeAllMembers().map { members ->
        members.groupBy(CreatorGroupMemberEntity::groupId)
            .mapValues { (_, groupMembers) -> groupMembers.mapTo(linkedSetOf(), CreatorGroupMemberEntity::mid) }
    }

    fun members(groupId: Long): Flow<List<CreatorGroupMemberEntity>> = dao.observeMembers(groupId)

    suspend fun create(name: String, position: Int = 0): Long {
        val normalized = name.trim()
        require(normalized.isNotEmpty()) { "分组名称不能为空" }
        val now = nowEpochMs()
        return dao.insertGroup(
            CreatorGroupEntity(
                name = normalized,
                position = position.coerceAtLeast(0),
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
        )
    }

    suspend fun rename(group: CreatorGroupEntity, name: String) {
        val normalized = name.trim()
        require(normalized.isNotEmpty()) { "分组名称不能为空" }
        dao.upsertGroup(group.copy(name = normalized, updatedAtEpochMs = nowEpochMs()))
    }

    suspend fun delete(groupId: Long) = dao.deleteGroup(groupId)

    suspend fun replaceMembers(groupId: Long, mids: List<Long>) {
        val now = nowEpochMs()
        dao.replaceMembers(
            groupId = groupId,
            members = mids.distinct().mapIndexed { index, mid ->
                CreatorGroupMemberEntity(
                    groupId = groupId,
                    mid = mid,
                    position = index,
                    addedAtEpochMs = now,
                )
            },
        )
    }

    suspend fun setMembership(groupId: Long, mid: Long, selected: Boolean) {
        if (selected) {
            dao.upsertMember(
                CreatorGroupMemberEntity(
                    groupId = groupId,
                    mid = mid,
                    position = Int.MAX_VALUE,
                    addedAtEpochMs = nowEpochMs(),
                ),
            )
        } else {
            dao.deleteMember(groupId, mid)
        }
    }
}

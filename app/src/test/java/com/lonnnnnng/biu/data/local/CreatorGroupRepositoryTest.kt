package com.lonnnnnng.biu.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CreatorGroupRepositoryTest {
    @Test
    fun `分组成员独立保存且保持用户排序`() = runBlocking {
        val dao = FakeCreatorGroupDao()
        val repository = CreatorGroupRepository(dao, nowEpochMs = { 1234L })

        val groupId = repository.create(" 翻唱 ")
        repository.replaceMembers(groupId, listOf(3002L, 3001L, 3002L))

        assertEquals("翻唱", repository.groups.first().single().name)
        assertEquals(listOf(3002L, 3001L), repository.members(groupId).first().map { it.mid })
        assertEquals(listOf(0, 1), repository.members(groupId).first().map { it.position })
    }

    private class FakeCreatorGroupDao : CreatorGroupDao {
        private val groupsFlow = MutableStateFlow<List<CreatorGroupEntity>>(emptyList())
        private val memberFlows = mutableMapOf<Long, MutableStateFlow<List<CreatorGroupMemberEntity>>>()
        private var nextId = 1L

        override fun observeGroups(): Flow<List<CreatorGroupEntity>> = groupsFlow

        override fun observeMembers(groupId: Long): Flow<List<CreatorGroupMemberEntity>> =
            memberFlows.getOrPut(groupId) { MutableStateFlow(emptyList()) }

        override fun observeAllMembers(): Flow<List<CreatorGroupMemberEntity>> = MutableStateFlow(
            memberFlows.values.flatMap { it.value },
        )

        override suspend fun insertGroup(group: CreatorGroupEntity): Long {
            val id = if (group.groupId == 0L) nextId++ else group.groupId
            groupsFlow.value = (groupsFlow.value + group.copy(groupId = id)).sortedBy { it.position }
            return id
        }

        override suspend fun upsertGroup(group: CreatorGroupEntity) {
            groupsFlow.value = (groupsFlow.value.filterNot { it.groupId == group.groupId } + group)
                .sortedBy { it.position }
        }

        override suspend fun deleteGroup(groupId: Long) {
            groupsFlow.value = groupsFlow.value.filterNot { it.groupId == groupId }
            memberFlows.remove(groupId)
        }

        override suspend fun insertMembers(members: List<CreatorGroupMemberEntity>) {
            val groupId = members.firstOrNull()?.groupId ?: return
            memberFlows.getOrPut(groupId) { MutableStateFlow(emptyList()) }.value = members.sortedBy { it.position }
        }

        override suspend fun upsertMember(member: CreatorGroupMemberEntity) {
            val flow = memberFlows.getOrPut(member.groupId) { MutableStateFlow(emptyList()) }
            flow.value = (flow.value.filterNot { it.mid == member.mid } + member).sortedBy { it.position }
        }

        override suspend fun deleteMembers(groupId: Long) {
            memberFlows.getOrPut(groupId) { MutableStateFlow(emptyList()) }.value = emptyList()
        }


        override suspend fun deleteMember(groupId: Long, mid: Long) {
            val flow = memberFlows.getOrPut(groupId) { MutableStateFlow(emptyList()) }
            flow.value = flow.value.filterNot { it.mid == mid }
        }
    }
}

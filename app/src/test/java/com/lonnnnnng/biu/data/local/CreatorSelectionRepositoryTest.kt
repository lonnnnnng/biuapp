package com.lonnnnnng.biu.data.local

import com.lonnnnnng.biu.data.bilibili.BilibiliCreator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CreatorSelectionRepositoryTest {
    @Test
    fun `保存UP范围后按选择顺序持续输出完整资料`() = runBlocking {
        val dao = FakeCreatorSelectionDao()
        val repository = CreatorSelectionRepository(dao)
        val selected = listOf(
            BilibiliCreator(mid = 1002L, name = "UP二", faceUrl = "https://example.com/2.jpg"),
            BilibiliCreator(mid = 1001L, name = "UP一", faceUrl = "https://example.com/1.jpg"),
        )

        repository.replaceAll(selected)

        assertEquals(selected, repository.selected.first())
        assertEquals(listOf(0, 1), dao.current.map(CreatorSelectionEntity::position))
    }

    private class FakeCreatorSelectionDao : CreatorSelectionDao {
        private val entities = MutableStateFlow<List<CreatorSelectionEntity>>(emptyList())
        val current: List<CreatorSelectionEntity>
            get() = entities.value

        override fun observeAll(): Flow<List<CreatorSelectionEntity>> = entities

        override suspend fun insertAll(entities: List<CreatorSelectionEntity>) {
            this.entities.value = entities.sortedBy(CreatorSelectionEntity::position)
        }

        override suspend fun deleteAll() {
            entities.value = emptyList()
        }
    }
}

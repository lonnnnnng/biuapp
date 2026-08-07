package com.lonnnnnng.biu.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreatorCenterSessionPolicyTest {
    @Test
    fun `会话往返保留筛选 当前UP 列表位置和未保存来源`() {
        val original = CreatorCenterSession(
            tab = "HOME_SELECTED",
            selectedGroupId = 7L,
            searchInput = "现场",
            searchKeyword = "现场音乐",
            filterKeyword = "周杰伦",
            selectedCreatorMid = 123L,
            selectedCreatorName = "测试 UP",
            selectedCreatorFaceUrl = "https://example.com/avatar.jpg",
            profileTab = "COLLECTIONS",
            listPositions = mapOf(
                CreatorCenterListSlot.SEARCH to PersistedListPosition(8, 24),
                CreatorCenterListSlot.COLLECTIONS to PersistedListPosition(3, 12),
            ),
            hasSourceDraft = true,
            sourceDraft = listOf(
                CreatorSourceDraftItem(123L, "测试 UP", "https://example.com/avatar.jpg"),
                CreatorSourceDraftItem(456L, "未关注 UP", ""),
            ),
        )

        val restored = CreatorCenterSessionPolicy.decode(CreatorCenterSessionPolicy.encode(original))

        assertEquals(original, restored)
    }

    @Test
    fun `损坏会话安全回退并清理非法位置和重复UP`() {
        val restored = CreatorCenterSessionPolicy.decode(
            """{
              "tab":"SEARCH",
              "selectedGroupId":-4,
              "selectedCreatorMid":0,
              "listPositions":{"SEARCH":{"index":-5,"offset":-8}},
              "hasSourceDraft":true,
              "sourceDraft":[
                {"mid":88,"name":"A","faceUrl":""},
                {"mid":88,"name":"B","faceUrl":""},
                {"mid":-1,"name":"非法","faceUrl":""}
              ]
            }""".trimIndent(),
        )

        assertNull(restored.selectedGroupId)
        assertNull(restored.selectedCreatorMid)
        assertEquals(PersistedListPosition(), restored.position(CreatorCenterListSlot.SEARCH))
        assertEquals(listOf(88L), restored.sourceDraft.map(CreatorSourceDraftItem::mid))
        assertEquals(CreatorCenterSession(), CreatorCenterSessionPolicy.decode("not-json"))
    }
}

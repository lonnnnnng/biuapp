package com.lonnnnnng.biu.ui

import com.lonnnnnng.biu.data.bilibili.BilibiliCreator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorSourceDraftPolicyTest {
    @Test
    fun `搜索到的未关注UP也能加入并保留选择顺序`() {
        val saved = listOf(creator(1L, "已关注"))
        val searched = creator(2L, "未关注")

        val draft = CreatorSourceDraftPolicy.toggle(saved, searched)

        assertEquals(listOf(1L, 2L), draft.map(BilibiliCreator::mid))
        assertTrue(CreatorSourceDraftPolicy.isDirty(draft, saved))
    }

    @Test
    fun `再次勾选移除来源且同一顺序视为没有修改`() {
        val saved = listOf(creator(1L, "A"), creator(2L, "B"))
        val removed = CreatorSourceDraftPolicy.toggle(saved, saved.last())

        assertEquals(listOf(1L), removed.map(BilibiliCreator::mid))
        assertTrue(CreatorSourceDraftPolicy.isDirty(removed, saved))
        assertFalse(CreatorSourceDraftPolicy.isDirty(saved.map { it.copy(name = "新名称") }, saved))
    }

    private fun creator(mid: Long, name: String) = BilibiliCreator(
        mid = mid,
        name = name,
        faceUrl = "",
    )
}

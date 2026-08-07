package com.lonnnnnng.biu.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchHistoryPolicyTest {
    @Test
    fun `新关键词置顶并忽略大小写重复`() {
        val result = SearchHistoryPolicy.record(
            current = listOf("周杰伦", "Taylor Swift", "老歌"),
            keyword = "  taylor swift  ",
        )

        assertEquals(listOf("taylor swift", "周杰伦", "老歌"), result)
    }

    @Test
    fun `搜索历史最多保留十条且损坏数据安全回退`() {
        val result = SearchHistoryPolicy.record((1..10).map(Int::toString), "新歌")

        assertEquals(10, result.size)
        assertEquals("新歌", result.first())
        assertEquals(emptyList<String>(), SearchHistoryPolicy.decode("not-json"))
    }

    @Test
    fun `编码后可按原顺序恢复`() {
        val history = listOf("中文 歌曲", "A/B", "换行\n关键词")

        assertEquals(history, SearchHistoryPolicy.decode(SearchHistoryPolicy.encode(history)))
    }
}

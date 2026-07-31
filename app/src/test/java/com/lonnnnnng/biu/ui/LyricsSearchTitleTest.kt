package com.lonnnnnng.biu.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsSearchTitleTest {
    @Test
    fun `单P优先从资源标题拆出歌曲和歌手`() {
        assertEquals(
            LyricsSearchDefaults("夜曲", "周杰伦"),
            lyricsSearchDefaults("夜曲-周杰伦", null, "视频 UP 主"),
        )
    }

    @Test
    fun `多P使用当前P名称并清理曲序`() {
        assertEquals(
            LyricsSearchDefaults("夜曲", "周杰伦"),
            lyricsSearchDefaults(
                title = "一人一首成名曲",
                pageTitle = "P1 · 001. 夜曲-周杰伦",
                fallbackArtist = "发狂的音乐细胞",
            ),
        )
    }

    @Test
    fun `多P缺少名称时回退资源标题`() {
        assertEquals(
            LyricsSearchDefaults("一人一首成名曲", "发狂的音乐细胞"),
            lyricsSearchDefaults("一人一首成名曲", "P1", "发狂的音乐细胞"),
        )
    }

    @Test
    fun `多P名称没有歌手时不拼接视频UP主`() {
        assertEquals(
            LyricsSearchDefaults("纯音乐", ""),
            lyricsSearchDefaults("合集标题", "P3 · 003：纯音乐", "视频 UP 主"),
        )
    }
}

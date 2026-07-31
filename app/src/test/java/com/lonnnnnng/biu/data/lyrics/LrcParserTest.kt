package com.lonnnnnng.biu.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {
    @Test
    fun `解析多时间标签并忽略元数据和空歌词`() {
        val lyrics = LrcParser.parse(
            """
            [ar:测试歌手]
            [00:03.5]第三句
            [00:01.25][00:02.125]重复句
            [00:04.00]
            [00:00]第一句
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                LyricsLine(0L, "第一句"),
                LyricsLine(1_250L, "重复句"),
                LyricsLine(2_125L, "重复句"),
                LyricsLine(3_500L, "第三句"),
            ),
            lyrics,
        )
    }

    @Test
    fun `按播放进度和偏移定位当前歌词`() {
        val lyrics = listOf(
            LyricsLine(1_000L, "第一句"),
            LyricsLine(2_000L, "第二句"),
            LyricsLine(3_000L, "第三句"),
        )

        assertEquals(-1, LrcParser.currentLineIndex(lyrics, positionMs = 600L, offsetMs = 300L))
        assertEquals(0, LrcParser.currentLineIndex(lyrics, positionMs = 700L, offsetMs = 300L))
        assertEquals(1, LrcParser.currentLineIndex(lyrics, positionMs = 2_400L, offsetMs = -300L))
        assertEquals(2, LrcParser.currentLineIndex(lyrics, positionMs = 9_000L))
    }
}

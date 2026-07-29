package com.lonnnnnng.biu.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackMediaMetadataTest {
    @Test
    fun `多P媒体标题使用当前分P名称并保留主标题`() {
        val metadata = track(
            title = "主视频标题 · 第二首",
            pageTitle = "P2 · 第二首",
        ).mediaText()

        assertEquals("第二首", metadata.title)
        assertEquals("主视频标题", metadata.albumTitle)
        assertEquals("P2 · 第二首", metadata.subtitle)
    }

    @Test
    fun `单P媒体标题保持主视频标题`() {
        val metadata = track(
            title = "单P视频标题",
            pageTitle = null,
        ).mediaText()

        assertEquals("单P视频标题", metadata.title)
        assertNull(metadata.albumTitle)
        assertNull(metadata.subtitle)
    }

    @Test
    fun `分P没有名称时锁屏显示P序号且主标题不混入兜底名称`() {
        val metadata = track(
            title = "主视频标题 · 第 1 P",
            pageTitle = "P1",
        ).mediaText()

        assertEquals("P1", metadata.title)
        assertEquals("主视频标题", metadata.albumTitle)
        assertEquals("P1", metadata.subtitle)
    }

    private fun track(title: String, pageTitle: String?) = Track(
        id = "BVTEST:100",
        title = title,
        artist = "测试作者",
        streamUrl = "https://example.com/audio.m4s",
        pageTitle = pageTitle,
    )
}

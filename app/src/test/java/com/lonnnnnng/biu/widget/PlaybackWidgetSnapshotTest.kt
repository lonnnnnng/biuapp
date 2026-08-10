package com.lonnnnnng.biu.widget

import com.lonnnnnng.biu.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlaybackWidgetSnapshotTest {
    @Test
    fun `Room恢复多P曲目时显示当前分P名称和视频主标题`() {
        val snapshot = PlaybackWidgetSnapshot.from(
            track = track(
                title = "主视频标题 · 第二首",
                pageTitle = "P2 · 第二首",
            ),
            positionMs = 90_000L,
            durationMs = 180_000L,
        )

        assertEquals("第二首", snapshot.title)
        assertEquals("主视频标题", snapshot.contextLabel)
        assertEquals("测试 UP 主", snapshot.artist)
        assertEquals(500, snapshot.progressPermille)
        assertFalse(snapshot.isPlaying)
    }

    @Test
    fun `Room恢复单P曲目时保持原始标题且不显示上下文`() {
        val snapshot = PlaybackWidgetSnapshot.from(
            track = track(title = "单P视频标题", pageTitle = null),
            positionMs = 10_000L,
            durationMs = 40_000L,
        )

        assertEquals("单P视频标题", snapshot.title)
        assertEquals("", snapshot.contextLabel)
        assertEquals(250, snapshot.progressPermille)
    }

    @Test
    fun `恢复进度处理未知时长和越界位置`() {
        assertEquals(0, playbackWidgetProgressPermille(20_000L, 0L))
        assertEquals(0, playbackWidgetProgressPermille(-1L, 40_000L))
        assertEquals(1_000, playbackWidgetProgressPermille(50_000L, 40_000L))
        assertEquals(1_000, playbackWidgetProgressPermille(Long.MAX_VALUE, Long.MAX_VALUE))
    }

    private fun track(title: String, pageTitle: String?) = Track(
        id = "BVTEST:200",
        title = title,
        artist = "测试 UP 主",
        streamUrl = "https://example.com/audio.m4s",
        artworkUrl = "https://example.com/cover.jpg",
        pageTitle = pageTitle,
    )
}

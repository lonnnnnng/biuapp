package com.lonnnnnng.biu.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoGridLayoutPolicyTest {
    @Test
    fun `手机宽度固定保持两列`() {
        assertEquals(2, VideoGridLayoutPolicy.columnCount(360.dp))
        assertEquals(2, VideoGridLayoutPolicy.columnCount(519.dp))
    }

    @Test
    fun `中等宽度使用三列`() {
        assertEquals(3, VideoGridLayoutPolicy.columnCount(520.dp))
        assertEquals(3, VideoGridLayoutPolicy.columnCount(759.dp))
    }

    @Test
    fun `横屏主内容使用四列`() {
        assertEquals(4, VideoGridLayoutPolicy.columnCount(760.dp))
        assertEquals(4, VideoGridLayoutPolicy.columnCount(840.dp))
    }
}

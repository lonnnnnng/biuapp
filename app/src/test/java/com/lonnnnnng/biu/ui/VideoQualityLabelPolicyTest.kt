package com.lonnnnnng.biu.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoQualityLabelPolicyTest {
    @Test
    fun `播放器画质只显示分辨率`() {
        assertEquals("720P", VideoQualityLabelPolicy.resolutionLabel("720p · AVC"))
        assertEquals("1080P", VideoQualityLabelPolicy.resolutionLabel("1080p · 60fps · HEVC"))
    }

    @Test
    fun `空画质回退通用文案`() {
        assertEquals("画质", VideoQualityLabelPolicy.resolutionLabel(""))
    }
}

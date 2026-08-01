package com.lonnnnnng.biu.ui

internal object VideoQualityLabelPolicy {
    fun resolutionLabel(rawLabel: String): String {
        val resolution = rawLabel.substringBefore(" · ").trim()
        if (resolution.isBlank()) return "画质"
        // long: 播放器工具栏空间有限，只保留用户切换时真正关心的分辨率；编解码和帧率仍可在画质菜单中查看。
        return resolution.replace(Regex("(?i)p$"), "P")
    }
}

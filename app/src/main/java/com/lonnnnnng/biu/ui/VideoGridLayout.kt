package com.lonnnnnng.biu.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object VideoGridLayoutPolicy {
    fun columnCount(availableWidth: Dp): Int = when {
        // long: 主内容最大宽度为 840dp；宽屏增加列数，避免横屏仍显示两张超宽卡片。
        availableWidth >= 760.dp -> 4
        availableWidth >= 520.dp -> 3
        else -> 2
    }
}

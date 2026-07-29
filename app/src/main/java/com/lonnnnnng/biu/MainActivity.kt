package com.lonnnnnng.biu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.lonnnnnng.biu.ui.BiuApp
import com.lonnnnnng.biu.ui.theme.BiuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // long: 应用固定使用深色界面，系统栏同步使用浅色图标，避免近黑背景下状态信息不可见。
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            BiuTheme {
                BiuApp()
            }
        }
    }
}

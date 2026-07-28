package com.lonnnnnng.biu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.lonnnnnng.biu.ui.BiuApp
import com.lonnnnnng.biu.ui.theme.BiuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BiuTheme {
                BiuApp()
            }
        }
    }
}

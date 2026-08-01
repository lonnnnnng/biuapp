package com.lonnnnnng.biu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lonnnnnng.biu.ui.BiuApp
import com.lonnnnnng.biu.ui.BiuViewModel
import com.lonnnnnng.biu.ui.theme.BiuTheme

class MainActivity : ComponentActivity() {
    private val viewModel: BiuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by viewModel.state.collectAsStateWithLifecycle()
            BiuTheme(
                themeMode = uiState.themeMode,
                textScale = uiState.textScale,
                listDensity = uiState.listDensity,
            ) {
                BiuApp(viewModel)
            }
        }
    }
}

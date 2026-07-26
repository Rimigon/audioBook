package com.nikit.audiobook.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.nikit.audiobook.ui.navigation.AppNav
import com.nikit.audiobook.ui.settings.SettingsViewModel
import com.nikit.audiobook.ui.theme.AudioBookTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val state by settingsVm.uiState.collectAsState()
            AudioBookTheme(mode = state.themeMode) {
                AppNav()
            }
        }
    }
}

package com.nikit.audiobook.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.nikit.audiobook.data.repo.BookRepository
import com.nikit.audiobook.ui.navigation.AppNav
import com.nikit.audiobook.ui.settings.SettingsViewModel
import com.nikit.audiobook.ui.theme.AudioBookTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var bookRepository: BookRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        repairStatusesOnce()
        setContent {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val state by settingsVm.uiState.collectAsState()
            AudioBookTheme(mode = state.themeMode) {
                AppNav()
            }
        }
    }

    /**
     * Одноразовая починка статусов: старые сборки помечали «читаю» ВСЕ импортированные
     * книги (без прогресса), из-за чего теги были неверными везде, а каталоги пустыми.
     * Возвращаем в «хочу» книги без реального прогресса. Запускается один раз.
     */
    private fun repairStatusesOnce() {
        val prefs = getSharedPreferences("audiobook", MODE_PRIVATE)
        if (prefs.getBoolean("statuses_fixed_v2", false)) return
        lifecycleScope.launch {
            bookRepository.normalizeStatuses()
            prefs.edit().putBoolean("statuses_fixed_v2", true).apply()
        }
    }
}

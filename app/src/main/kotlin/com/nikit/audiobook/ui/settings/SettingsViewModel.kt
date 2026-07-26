package com.nikit.audiobook.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikit.audiobook.data.saf.ScanFacade
import com.nikit.audiobook.data.saf.ScanSettings
import com.nikit.audiobook.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val scanFacade: ScanFacade,
        private val scanSettings: ScanSettings,
    ) : ViewModel() {
        val scanning = MutableStateFlow(false)
        val lastMessage = MutableStateFlow<String?>(null)

        val uiState: StateFlow<SettingsUiState> =
            combine(
                scanSettings.treeUri,
                scanSettings.themeMode,
                scanning,
                lastMessage,
            ) { tree, theme, scanning, msg ->
                SettingsUiState(treeUri = tree, themeMode = theme, scanning = scanning, message = msg)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

        fun setTreeUri(uri: Uri) =
            viewModelScope.launch {
                scanSettings.setTreeUri(uri.toString())
                scanNow(uri)
            }

        fun scanNow() =
            viewModelScope.launch {
                val s = uiState.value
                val uriStr =
                    s.treeUri ?: run {
                        lastMessage.value = "Сначала выберите папку"
                        return@launch
                    }
                scanNow(Uri.parse(uriStr))
            }

        private suspend fun scanNow(uri: Uri) {
            scanning.value = true
            lastMessage.value = null
            runCatching { scanFacade.scanNow(uri) }
                .onSuccess { added -> lastMessage.value = "Добавлено книг: $added" }
                .onFailure { lastMessage.value = "Ошибка скана: ${it.message}" }
            scanning.value = false
        }

        fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { scanSettings.setThemeMode(mode) }
    }

data class SettingsUiState(
    val treeUri: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val scanning: Boolean = false,
    val message: String? = null,
)

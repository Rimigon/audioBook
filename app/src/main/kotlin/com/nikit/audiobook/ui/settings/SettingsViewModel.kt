package com.nikit.audiobook.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikit.audiobook.data.saf.ScanFacade
import com.nikit.audiobook.data.saf.ScanSettings
import com.nikit.audiobook.player.controller.PlayerSettings
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
                scanSettings.seekStepMs,
                scanSettings.autoResume,
                scanSettings.onlineEnrich,
                scanSettings.googleBooksKey,
                scanSettings.rescanIntervalMin,
            ) { values ->
                SettingsUiState(
                    treeUri = values[0] as String?,
                    themeMode = values[1] as ThemeMode,
                    scanning = values[2] as Boolean,
                    message = values[3] as String?,
                    seekStepMs = values[4] as Long,
                    autoResume = values[5] as Boolean,
                    onlineEnrich = values[6] as Boolean,
                    googleBooksKey = values[7] as String?,
                    rescanIntervalMin = values[8] as Int,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

        init {
            applyRuntimeSettings()
        }

        private fun applyRuntimeSettings() =
            viewModelScope.launch {
                scanSettings.seekStepMs.collect { PlayerSettings.seekStepMs = it }
                scanSettings.autoResume.collect { PlayerSettings.autoResume = it }
                scanSettings.onlineEnrich.collect { PlayerSettings.onlineEnrichment = it }
            }

        fun setTreeUri(uri: Uri) =
            viewModelScope.launch {
                scanSettings.setTreeUri(uri.toString())
                scanNow(uri)
            }

        fun scanNow() =
            viewModelScope.launch {
                val uriStr =
                    uiState.value.treeUri ?: run {
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

        fun setSeekStep(ms: Long) = viewModelScope.launch { scanSettings.setSeekStep(ms) }

        fun setAutoResume(v: Boolean) = viewModelScope.launch { scanSettings.setAutoResume(v) }

        fun setOnlineEnrich(v: Boolean) = viewModelScope.launch { scanSettings.setOnlineEnrich(v) }

        fun setGoogleBooksKey(v: String) = viewModelScope.launch { scanSettings.setGoogleBooksKey(v) }

        fun setRescanInterval(min: Int) = viewModelScope.launch { scanSettings.setRescanInterval(min) }
    }

data class SettingsUiState(
    val treeUri: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val scanning: Boolean = false,
    val message: String? = null,
    val seekStepMs: Long = 30_000L,
    val autoResume: Boolean = true,
    val onlineEnrich: Boolean = true,
    val googleBooksKey: String? = null,
    val rescanIntervalMin: Int = 60,
)

package com.nikit.audiobook.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikit.audiobook.data.diag.CrashLogger
import com.nikit.audiobook.data.saf.ScanFacade
import com.nikit.audiobook.data.saf.ScanSettings
import com.nikit.audiobook.player.controller.PlayerSettings
import com.nikit.audiobook.player.work.RescanScheduler
import com.nikit.audiobook.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val scanFacade: ScanFacade,
        private val scanSettings: ScanSettings,
        private val rescanScheduler: RescanScheduler,
        private val crashLogger: CrashLogger,
    ) : ViewModel() {
        val scanning = MutableStateFlow(false)
        val lastMessage = MutableStateFlow<String?>(null)

        @Suppress("UNCHECKED_CAST")
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
            // Проверяем, что у нас всё ещё есть persistable-грант на дерево:
            // без него DocumentFile.list() вернёт пусто, и рескан «не найдёт» книги.
            val granted =
                context.contentResolver.persistedUriPermissions.any {
                    it.uri == uri && it.isReadPermission
                }
            if (!granted) {
                lastMessage.value = "Нет доступа к папке — выберите её заново"
                crashLogger.write(RuntimeException("SCAN-TRACE: grant missing for $uri"))
                return
            }
            scanning.value = true
            lastMessage.value = null
            val result =
                withContext(Dispatchers.IO) {
                    runCatching { scanFacade.scanNow(uri) }
                }
            result
                .onSuccess { r ->
                    crashLogger.write(
                        RuntimeException(
                            "SCAN-TRACE: uri=$uri found=${r.found} added=${r.added} skipped=${r.skipped} failures=${r.failures}",
                        ),
                    )
                    lastMessage.value =
                        when {
                            r.found == 0 -> "Книги не найдены: папка пуста или нет доступа к файлам"
                            r.added > 0 -> "Найдено: ${r.found} · добавлено: ${r.added}"
                            else -> "Найдено: ${r.found} новых нет (уже в каталоге)"
                        } + if (r.failures.isNotEmpty()) " · ошибок: ${r.failures.size}" else ""
                }.onFailure { e ->
                    crashLogger.write(RuntimeException("SCAN-TRACE: exception ${e.message}", e))
                    lastMessage.value = "Ошибка скана: ${e.message}"
                }
            scanning.value = false
        }

        fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { scanSettings.setThemeMode(mode) }

        fun setSeekStep(ms: Long) = viewModelScope.launch { scanSettings.setSeekStep(ms) }

        fun setAutoResume(v: Boolean) = viewModelScope.launch { scanSettings.setAutoResume(v) }

        fun setOnlineEnrich(v: Boolean) = viewModelScope.launch { scanSettings.setOnlineEnrich(v) }

        fun setGoogleBooksKey(v: String) = viewModelScope.launch { scanSettings.setGoogleBooksKey(v) }

        fun setRescanInterval(min: Int) =
            viewModelScope.launch {
                scanSettings.setRescanInterval(min)
                rescanScheduler.schedule(min)
            }

        // --- Журнал крашей ---
        data class CrashLogEntry(
            val file: File,
            val time: String,
            val preview: String,
        )

        val crashLogs = MutableStateFlow<List<CrashLogEntry>>(emptyList())

        fun refreshCrashLogs() {
            crashLogs.value =
                crashLogger.logs().map { f ->
                    CrashLogEntry(
                        file = f,
                        time = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(f.lastModified())),
                        preview =
                            crashLogger.read(f).lineSequence().firstOrNull { "Exception" in it || "Error" in it }
                                ?: "—",
                    )
                }
        }

        fun readCrashLog(file: File): String = crashLogger.read(file)

        fun clearCrashLogs() {
            crashLogger.clear()
            refreshCrashLogs()
        }
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

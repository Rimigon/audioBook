package com.nikit.audiobook.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nikit.audiobook.data.saf.humanPath
import com.nikit.audiobook.ui.settings.SettingsViewModel.CrashLogEntry
import com.nikit.audiobook.ui.theme.ThemeMode
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    val pickTree =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                // Сохраняем грант на чтение И запись дерева, чтобы рескан, фоновый Worker
                // и удаление файлов могли обращаться к папке после перезапуска процесса.
                // Без FLAG_GRANT_WRITE_URI_PERMISSION удаление файлов падает с SecurityException.
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                }
                vm.setTreeUri(uri)
            }
        }
    var gbKey by remember(state.googleBooksKey) { mutableStateOf(state.googleBooksKey ?: "") }
    val crashLogs by vm.crashLogs.collectAsState()
    var showCrashLogs by remember { mutableStateOf(false) }
    var selectedCrash by remember { mutableStateOf<File?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Настройки", style = MaterialTheme.typography.headlineMedium) }) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Section(title = "Источник") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        state.treeUri
                            ?.let {
                                runCatching {
                                    android.net.Uri
                                        .parse(it)
                                        .humanPath()
                                }.getOrNull()
                            }
                            ?: "не выбрана",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    androidx.compose.material3.Button(
                        onClick = { pickTree.launch(null) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Выбрать папку и сканировать") }
                    OutlinedButton(
                        onClick = { vm.scanNow() },
                        enabled = !state.scanning,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (state.scanning) "Сканирую…" else "Сканировать снова") }
                    state.message?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }

            Section(title = "Внешний вид") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Тема", style = MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = state.themeMode == mode,
                                onClick = { vm.setThemeMode(mode) },
                                label = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.LIGHT -> "Светлая"
                                            ThemeMode.DARK -> "Тёмная"
                                            ThemeMode.SYSTEM -> "Системная"
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }

            Section(title = "Воспроизведение") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Шаг перемотки", style = MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10_000L, 30_000L, 60_000L).forEach { step ->
                            FilterChip(
                                selected = state.seekStepMs == step,
                                onClick = { vm.setSeekStep(step) },
                                label = { Text("${step / 1000}с") },
                            )
                        }
                    }
                    ToggleRow(
                        label = "Авто-резьюм",
                        checked = state.autoResume,
                        onCheckedChange = { vm.setAutoResume(it) },
                    )
                }
            }

            Section(title = "Метаданные") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToggleRow(
                        label = "Онлайн-обогащение метаданных",
                        checked = state.onlineEnrich,
                        onCheckedChange = { vm.setOnlineEnrich(it) },
                    )
                    Text("Ключ Google Books (необязательно)", style = MaterialTheme.typography.bodyLarge)
                    OutlinedTextField(
                        value = gbKey,
                        onValueChange = {
                            gbKey = it
                            vm.setGoogleBooksKey(it)
                        },
                        label = { Text("API key") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Интервал автоскана", style = MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 60, 240).forEach { min ->
                            FilterChip(
                                selected = state.rescanIntervalMin == min,
                                onClick = { vm.setRescanInterval(min) },
                                label = { Text(if (min < 60) "${min}м" else "${min / 60}ч") },
                            )
                        }
                    }
                }
            }

            Section(title = "О приложении") {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Аудиокниги · версия 0.1.0", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Личное офлайн-приложение. Метаданные: OpenLibrary, Google Books.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Section(title = "Диагностика") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Вылеты пишутся в локальный журнал (без отправки в интернет).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = {
                            vm.refreshCrashLogs()
                            showCrashLogs = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Журнал ошибок") }
                }
            }
        }
    }

    if (showCrashLogs) {
        AlertDialog(
            onDismissRequest = { showCrashLogs = false },
            title = { Text("Журнал ошибок") },
            text = {
                if (crashLogs.isEmpty()) {
                    Text("Вылетов пока не было.")
                } else {
                    LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(crashLogs, key = { it.file.name }) { entry ->
                            Card(
                                onClick = { selectedCrash = entry.file },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Text(entry.time, style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        entry.preview,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (crashLogs.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                val all =
                                    crashLogs.joinToString("\n\n__________\n\n") { entry ->
                                        val t = vm.readCrashLog(entry.file)
                                        if (t.isEmpty()) entry.preview else t
                                    }
                                val cm =
                                    context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as
                                        android.content.ClipboardManager
                                cm.setPrimaryClip(android.content.ClipData.newPlainText("audiobook-errors", all))
                            },
                        ) { Text("Скопировать всё") }
                    }
                    TextButton(onClick = { showCrashLogs = false }) { Text("Закрыть") }
                }
            },
            dismissButton = {
                if (crashLogs.isNotEmpty()) {
                    TextButton(onClick = { vm.clearCrashLogs() }) { Text("Очистить") }
                }
            },
        )
    }

    selectedCrash?.let { file ->
        AlertDialog(
            onDismissRequest = { selectedCrash = null },
            title = { Text("Ошибка") },
            text = {
                Text(
                    vm.readCrashLog(file),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
            },
            confirmButton = { TextButton(onClick = { selectedCrash = null }) { Text("Закрыть") } },
        )
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

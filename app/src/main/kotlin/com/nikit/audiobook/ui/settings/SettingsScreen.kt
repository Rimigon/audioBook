package com.nikit.audiobook.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nikit.audiobook.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsState()
    val pickTree =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            if (uri != null) vm.setTreeUri(uri)
        }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Настройки", style = MaterialTheme.typography.headlineMedium) }) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Папка автоскана", style = MaterialTheme.typography.titleMedium)
            Text(
                state.treeUri ?: "не выбрана",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = { pickTree.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                Text("Выбрать папку и сканировать")
            }
            OutlinedButton(onClick = { vm.scanNow() }, enabled = !state.scanning, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.scanning) "Сканирую…" else "Сканировать снова")
            }
            state.message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }

            Text("Тема", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    OutlinedButton(onClick = { vm.setThemeMode(mode) }) {
                        Text(
                            when (mode) {
                                ThemeMode.LIGHT -> "Светлая"
                                ThemeMode.DARK -> "Тёмная"
                                ThemeMode.SYSTEM -> "Системная"
                            } +
                                if (state.themeMode == mode) " ✓" else "",
                        )
                    }
                }
            }

            Text("О приложении", style = MaterialTheme.typography.titleMedium)
            Text(
                "Аудиокниги · версия 0.1.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Личное офлайн-приложение. Метаданные онлайн: OpenLibrary, Google Books.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

package com.nikit.audiobook.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nikit.audiobook.ui.common.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    vm: PlayerViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title ?: "Плеер") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.addBookmark("Закладка") }) {
                        Icon(Icons.Filled.Bookmark, contentDescription = "Закладка")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(state.title ?: "—", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Глава ${state.chapterIndex + 1}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val dur = state.durationMs.coerceAtLeast(1L)
            Text(
                "${formatTime(state.positionMs)} / ${formatTime(state.durationMs)}",
                style = MaterialTheme.typography.labelLarge,
            )
            Slider(
                value = state.positionMs.toFloat().coerceIn(0f, dur.toFloat()),
                onValueChange = { vm.seek(it.toLong()) },
                valueRange = 0f..dur.toFloat(),
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { vm.previousChapter() }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Пред. глава")
                }
                IconButton(onClick = { vm.seekBack() }) {
                    Icon(Icons.Filled.FastRewind, contentDescription = "Назад 30с")
                }
                IconButton(onClick = { if (state.isPlaying) vm.pause() else vm.resume() }) {
                    Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Play/Pause")
                }
                IconButton(onClick = { vm.seekForward() }) {
                    Icon(Icons.Filled.FastForward, contentDescription = "Вперёд 30с")
                }
                IconButton(onClick = { vm.nextChapter() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "След. глава")
                }
            }

            Text("Скорость", style = MaterialTheme.typography.labelLarge)
            Slider(value = state.speed, onValueChange = { vm.setSpeed(it) }, valueRange = 0.5f..4.0f)
            Text("Усиление громкости", style = MaterialTheme.typography.labelLarge)
            Slider(value = state.volumeBoost, onValueChange = { vm.setVolumeBoost(it) }, valueRange = 1.0f..2.0f)

            Text("Таймер сна", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { vm.startSleep(5 * 60_000L) }, label = { Text("5м") })
                AssistChip(onClick = { vm.startSleep(15 * 60_000L) }, label = { Text("15м") })
                AssistChip(onClick = { vm.startSleep(30 * 60_000L) }, label = { Text("30м") })
                AssistChip(onClick = { vm.startSleep(60 * 60_000L) }, label = { Text("60м") })
            }
            state.sleepLeftMs?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text("Сон: ${formatTime(it)}") })
                    OutlinedButton(onClick = { vm.cancelSleep() }) { Text("Отмена") }
                }
            }
        }
    }
}

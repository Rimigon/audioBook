package com.nikit.audiobook.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nikit.audiobook.player.effects.EqualizerPreset
import com.nikit.audiobook.ui.common.BookCover
import com.nikit.audiobook.ui.common.formatTime

private val SPEED_PRESETS = listOf(0.8f, 1.0f, 1.25f, 1.5f, 2.0f)
private val BOOST_PRESETS = listOf(1.0f, 1.25f, 1.5f, 2.0f)
private val EQ_PRESETS = EqualizerPreset.entries
private val SLEEP_PRESETS = listOf(5, 15, 30, 60)

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
                title = { Text("Плеер") },
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
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BookCover(
                title = state.title ?: "",
                coverPath = state.coverPath,
                modifier =
                    Modifier
                        .size(width = 220.dp, height = 320.dp)
                        .shadow(8.dp, RoundedCornerShape(10.dp)),
            )

            Text(
                state.title ?: "—",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            state.author?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Text(
                    "Глава ${state.chapterIndex + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }

            Spacer(Modifier.height(6.dp))

            // Полоса перемотки — в пределах текущей главы. Так перемотка предсказуема
            // и не «переключает» главу при тапе; переход между главами — кнопками skip.
            val dur = state.durationMs.coerceAtLeast(1L)
            // Локальное значение во время перетаскивания: обновляем UI без seek на каждый
            // пиксель — seek делаем только когда пользователь отпустил ползунок.
            var dragMs by remember { mutableLongStateOf(-1L) }
            val displayPos = if (dragMs >= 0) dragMs else state.positionMs
            var showJump by remember { mutableStateOf(false) }
            Column(Modifier.fillMaxWidth()) {
                Slider(
                    value = displayPos.toFloat().coerceIn(0f, dur.toFloat()),
                    onValueChange = { dragMs = it.toLong() },
                    onValueChangeFinished = {
                        if (dragMs >= 0) {
                            vm.seek(dragMs)
                            dragMs = -1L
                        }
                    },
                    valueRange = 0f..dur.toFloat(),
                    colors =
                        SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        formatTime(displayPos),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = { showJump = true }) { Text("Перейти к времени") }
                    Text(
                        formatTime(state.durationMs),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Глобальный (сквозной по книге) прогресс — тонкий индикатор под слайдером.
                if (state.globalDurationMs > 0) {
                    val gPct =
                        (state.globalPositionMs.toFloat() / state.globalDurationMs).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { gPct },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(3.dp),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Книга: ${formatTime(state.globalPositionMs)} / ${formatTime(state.globalDurationMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "${(gPct * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (showJump) {
                JumpToTimeDialog(
                    durationMs = state.durationMs,
                    onDismiss = { showJump = false },
                    onSeek = { ms ->
                        vm.seek(ms)
                        showJump = false
                    },
                )
            }

            // Управление
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(onClick = { vm.previousChapter() }, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Пред. глава", modifier = Modifier.size(30.dp))
                }
                IconButton(onClick = { vm.seekBack() }, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Filled.FastRewind, contentDescription = "Назад 30с", modifier = Modifier.size(30.dp))
                }
                FilledIconButton(
                    onClick = { if (state.isPlaying) vm.pause() else vm.resume() },
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(38.dp),
                    )
                }
                IconButton(onClick = { vm.seekForward() }, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Filled.FastForward, contentDescription = "Вперёд 30с", modifier = Modifier.size(30.dp))
                }
                IconButton(onClick = { vm.nextChapter() }, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "След. глава", modifier = Modifier.size(30.dp))
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)

            SettingsSection(title = "Скорость") {
                ChipRow(
                    options = SPEED_PRESETS.map { it },
                    selected = { preset -> approxEq(preset, state.speed) },
                    label = { speedLabel(it) },
                    onSelect = { vm.setSpeed(it) },
                )
            }

            SettingsSection(title = "Усиление громкости") {
                ChipRow(
                    options = BOOST_PRESETS.map { it },
                    selected = { preset -> approxEq(preset, state.volumeBoost) },
                    label = { "×$it" },
                    onSelect = { vm.setVolumeBoost(it) },
                )
            }

            SettingsSection(title = "Эквалайзер") {
                ChipRow(
                    options = EQ_PRESETS.map { it },
                    selected = { state.equalizerPreset == it },
                    label = { eqLabel(it) },
                    onSelect = { vm.setEqualizer(it) },
                )
            }

            SettingsSection(title = "Таймер сна") {
                ChipRow(
                    options = SLEEP_PRESETS.map { it },
                    selected = { state.sleepLeftMs != null },
                    label = { sleepLabel(it) },
                    onSelect = { vm.startSleep(it * 60_000L) },
                )
                state.sleepLeftMs?.let { left ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Сон: ${formatTime(left)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(onClick = { vm.cancelSleep() }) { Text("Отмена") }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        content()
    }
}

@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: (T) -> Boolean,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            val isSel = selected(option)
            Surface(
                onClick = { onSelect(option) },
                shape = RoundedCornerShape(50),
                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

private fun speedLabel(v: Float): String = if (v == 1.0f) "1×" else "$v×"

private fun eqLabel(preset: EqualizerPreset): String =
    when (preset) {
        EqualizerPreset.FLAT -> "Без эффектов"
        EqualizerPreset.BASS_BOOST -> "Бас"
        EqualizerPreset.VOICE_CLARITY -> "Голос"
    }

private fun sleepLabel(min: Int): String = if (min < 60) "${min}м" else "${min / 60}ч"

private fun approxEq(
    a: Float,
    b: Float,
): Boolean = kotlin.math.abs(a - b) < 0.01f

/**
 * Парсит введённое время в миллисекунды. Поддерживает:
 * - «:ss» / «s» — секунды;
 * - «m:ss» / «m:ss.s» — минуты:секунды;
 * - «h:mm:ss» — часы:минуты:секунды.
 * Возвращает null при некорректном вводе.
 */
fun parseTimeToMs(input: String): Long? {
    val s = input.trim().replace(',', '.')
    if (s.isEmpty()) return null
    val parts = s.split(':')
    val nums = parts.mapNotNull { it.toDoubleOrNull() }
    if (nums.size != parts.size || nums.isEmpty()) return null
    val totalSec =
        when (nums.size) {
            1 -> nums[0]
            2 -> nums[0] * 60.0 + nums[1]
            3 -> nums[0] * 3600.0 + nums[1] * 60.0 + nums[2]
            else -> return null
        }
    if (totalSec < 0) return null
    return (totalSec * 1000).toLong()
}

@Composable
private fun JumpToTimeDialog(
    durationMs: Long,
    onDismiss: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val parsed = parseTimeToMs(text)
    val error =
        when {
            text.isNotEmpty() && parsed == null -> "Формат: чч:мм:сс или мм:сс"
            parsed != null && durationMs > 0 && parsed > durationMs -> "Вне длительности книги"
            else -> null
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Перейти к времени") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text("Например 1:23:45") },
                    isError = error != null,
                )
                error?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
                if (durationMs > 0) {
                    Text(
                        "Длительность главы: ${formatTime(durationMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsed != null && error == null,
                onClick = { parsed?.let(onSeek) },
            ) { Text("Перейти") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

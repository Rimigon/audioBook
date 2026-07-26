package com.nikit.audiobook.ui.book

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nikit.audiobook.ui.common.BookCover
import com.nikit.audiobook.ui.common.formatDuration
import com.nikit.audiobook.ui.common.formatTime
import com.nikit.audiobook.ui.theme.Missing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
    vm: BookDetailViewModel = hiltViewModel(),
) {
    val ui by vm.state.collectAsState()
    var confirmCatalog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ui?.book?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        val data = ui
        if (data == null) {
            Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Книга не найдена", Modifier.padding(24.dp))
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    BookCover(data.book.title, data.book.coverPath, Modifier.height(200.dp).width(140.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(data.book.title, style = MaterialTheme.typography.headlineMedium)
                    data.book.author?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!data.book.filesPresent) {
                        Text("нет на устройстве", style = MaterialTheme.typography.labelLarge, color = Missing)
                    }
                    if (data.book.totalDurationMs > 0) {
                        Text(
                            formatDuration(data.book.totalDurationMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    data.book.description?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item {
                if (data.progress != null) {
                    val pct = (data.progress.percent.coerceIn(0f, 1f))
                    Text(
                        "Прогресс: ${formatTime(data.progress.positionMs)} / ${formatTime(data.book.totalDurationMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                }
                Button(
                    onClick = { onPlay(data.book.id) },
                    enabled = data.book.filesPresent,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (data.progress != null) "Продолжить" else "Слушать")
                }
            }
            item {
                Text("Главы (${data.chapters.size})", style = MaterialTheme.typography.titleMedium)
                data.chapters.forEach { ch ->
                    Text(
                        "${ch.index + 1}. ${ch.title}  ${formatDuration(ch.endMs - ch.startMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
            if (data.bookmarks.isNotEmpty()) {
                item {
                    Text("Закладки", style = MaterialTheme.typography.titleMedium)
                    data.bookmarks.forEach { bm ->
                        Text(
                            "• ${bm.title} — ${formatTime(bm.positionMs)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { vm.deleteFiles() }, modifier = Modifier.weight(1f)) {
                        Text(if (data.book.filesPresent) "Удалить с устройства" else "Уже удалено")
                    }
                    OutlinedButton(onClick = { confirmCatalog = true }, modifier = Modifier.weight(1f)) {
                        Text("Удалить из каталога")
                    }
                }
            }
        }
    }

    if (confirmCatalog) {
        AlertDialog(
            onDismissRequest = { confirmCatalog = false },
            title = { Text("Удалить навсегда?") },
            text = {
                Text(
                    "Книга будет удалена из каталога вместе с прогрессом, закладками и привязкой к полкам. Это действие необратимо.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmCatalog = false
                    vm.deleteFromCatalog()
                    onBack()
                }) { Text("Удалить навсегда") }
            },
            dismissButton = { TextButton(onClick = { confirmCatalog = false }) { Text("Отмена") } },
        )
    }
}

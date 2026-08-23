package com.nikit.audiobook.ui.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nikit.audiobook.domain.model.Book
import com.nikit.audiobook.domain.model.BookStatus
import com.nikit.audiobook.domain.model.PlaybackProgress
import com.nikit.audiobook.ui.common.BookCover
import com.nikit.audiobook.ui.common.formatDuration
import com.nikit.audiobook.ui.common.formatTime
import com.nikit.audiobook.ui.common.statusColor
import com.nikit.audiobook.ui.common.statusLabel
import com.nikit.audiobook.ui.theme.Missing

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    onBookClick: (String) -> Unit,
    onRescanClick: () -> Unit,
    vm: LibraryViewModel = hiltViewModel(),
) {
    val books by vm.books.collectAsState()
    val progress by vm.progress.collectAsState()
    val dismissed by vm.dismissedContinue.collectAsState()
    val sort by vm.sort.collectAsState()
    val statusFilter by vm.statusFilter.collectAsState()
    val onlyPresent by vm.showOnlyPresent.collectAsState()
    val query by vm.searchQuery.collectAsState()
    val continueBooks =
        books.filter { b ->
            val p = progress[b.id]
            p != null && p.percent in 0f..0.99f && b.id !in dismissed
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Библиотека", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = { vm.setSort(nextSort(sort)) }) {
                        Icon(Icons.Default.Sort, contentDescription = "Сортировка")
                    }
                    IconButton(onClick = { vm.toggleOnlyPresent() }) {
                        Text(if (onlyPresent) "★" else "☆")
                    }
                    IconButton(onClick = onRescanClick) {
                        Icon(Icons.Default.Refresh, contentDescription = "Сканировать")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setSearchQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                placeholder = { Text("Поиск по названию или автору") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.setSearchQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Очистить")
                        }
                    }
                },
                singleLine = true,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { vm.setStatusFilter(null) },
                    label = { Text("Все") },
                )
                BookStatus.entries.forEach { s ->
                    FilterChip(
                        selected = statusFilter == s,
                        onClick = { vm.setStatusFilter(if (statusFilter == s) null else s) },
                        label = { Text(statusLabel(s)) },
                    )
                }
            }
            if (books.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Библиотека пуста.", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Добавьте папку с аудиокнигами в Настройках и нажмите «Сканировать».",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Карусель «Продолжить» — просто первый элемент списка: прокручивается вместе с ним.
                    if (continueBooks.isNotEmpty()) {
                        item(key = "continue-header") {
                            Text(
                                "Продолжить",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }
                        item(key = "continue-row") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(continueBooks, key = { it.id }) { book ->
                                    ContinueCard(
                                        book = book,
                                        progress = progress[book.id]!!,
                                        onClick = { onBookClick(book.id) },
                                        onDismiss = { vm.dismissFromContinue(book.id) },
                                    )
                                }
                            }
                        }
                    }
                    items(books, key = { it.id }) { book ->
                        BookRow(book, progress[book.id]) { onBookClick(book.id) }
                    }
                }
            }
        }
    }
}

private fun nextSort(s: LibrarySort): LibrarySort =
    when (s) {
        LibrarySort.RECENT -> LibrarySort.TITLE
        LibrarySort.TITLE -> LibrarySort.AUTHOR
        LibrarySort.AUTHOR -> LibrarySort.PROGRESS
        LibrarySort.PROGRESS -> LibrarySort.RECENT
    }

@Composable
private fun ContinueCard(
    book: Book,
    progress: PlaybackProgress,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(110.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box {
            Column(Modifier.padding(8.dp)) {
                BookCover(
                    book.title,
                    book.coverPath,
                    Modifier.fillMaxWidth().aspectRatio(2f / 3f),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    book.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "с ${formatTime(progress.positionMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress.percent.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                )
            }
            // Крестик скрывает книгу из ряда «Продолжить» (прогресс при этом сохраняется).
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).size(28.dp),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Убрать из «Продолжить»",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
internal fun BookRow(
    book: Book,
    progress: PlaybackProgress?,
    onClick: () -> Unit,
) {
    val started = progress != null && progress.percent > 0f && progress.percent < 0.99f
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookCover(book.title, book.coverPath, Modifier.size(width = 64.dp, height = 96.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                book.author?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                if (!book.filesPresent) {
                    Text(
                        "● нет на устройстве",
                        style = MaterialTheme.typography.labelMedium,
                        color = Missing,
                    )
                } else if (started) {
                    Text(
                        "Продолжить с ${formatTime(progress!!.positionMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        "Новая",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (book.totalDurationMs > 0) {
                    Text(
                        formatDuration(book.totalDurationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    statusLabel(book.status),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor(book.status),
                )
                if (started) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress!!.percent.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                    )
                }
            }
        }
    }
}

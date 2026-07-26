package com.nikit.audiobook.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.nikit.audiobook.ui.common.BookCover
import com.nikit.audiobook.ui.common.formatDuration
import com.nikit.audiobook.ui.theme.Missing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBookClick: (String) -> Unit,
    onRescanClick: () -> Unit,
    vm: LibraryViewModel = hiltViewModel(),
) {
    val books by vm.books.collectAsState()
    val sort by vm.sort.collectAsState()
    val onlyPresent by vm.showOnlyPresent.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Библиотека", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = {
                        // цикл сортировки
                        vm.setSort(nextSort(sort))
                    }) {
                        Icon(Icons.Default.Sort, contentDescription = "Сортировка")
                    }
                    IconButton(onClick = { vm.toggleOnlyPresent() }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Только на устройстве",
                            tint = if (onlyPresent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onRescanClick) {
                        Icon(Icons.Default.Refresh, contentDescription = "Сканировать")
                    }
                },
            )
        },
    ) { padding ->
        if (books.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
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
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(books, key = { it.id }) { book -> BookRow(book) { onBookClick(book.id) } }
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
private fun BookRow(
    book: Book,
    onClick: () -> Unit,
) {
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
            BookCover(book.title, book.coverPath, Modifier.padding(end = 0.dp))
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
                    )
                }
                if (!book.filesPresent) {
                    Text(
                        "нет на устройстве",
                        style = MaterialTheme.typography.labelSmall,
                        color = Missing,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (book.totalDurationMs > 0) {
                    Text(
                        formatDuration(book.totalDurationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

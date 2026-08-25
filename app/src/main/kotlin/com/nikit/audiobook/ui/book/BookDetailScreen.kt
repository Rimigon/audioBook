package com.nikit.audiobook.ui.book

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nikit.audiobook.domain.model.BookStatus
import com.nikit.audiobook.domain.model.Bookmark
import com.nikit.audiobook.domain.model.Chapter
import com.nikit.audiobook.domain.model.PlaybackProgress
import com.nikit.audiobook.ui.common.BookCover
import com.nikit.audiobook.ui.common.formatDuration
import com.nikit.audiobook.ui.common.formatTime
import com.nikit.audiobook.ui.common.statusColor
import com.nikit.audiobook.ui.common.statusLabel
import com.nikit.audiobook.ui.theme.Missing

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun BookDetailScreen(
    onBack: () -> Unit,
    onPlay: (String, Int?, Long?) -> Unit,
    vm: BookDetailViewModel = hiltViewModel(),
) {
    val ui by vm.state.collectAsState()
    var confirmCatalog by remember { mutableStateOf(false) }
    var showBookmarkDialog by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    // Своё состояние скролла на вкладку, чтобы позиция сохранялась при переключении.
    val chaptersState = remember { LazyListState() }
    val sessionsState = remember { LazyListState() }
    val bookmarksState = remember { LazyListState() }

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
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Шапка всегда развёрнута: название и обложка не уменьшаются при скролле.
            BookHeader(
                data = data,
                onPlay = { onPlay(data.book.id, null, null) },
                onAddBookmark = { showBookmarkDialog = true },
                onSetStatus = vm::setStatus,
            )
            PrimaryScrollableTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Главы") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Точки прослушивания") },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Закладки") },
                )
            }
            when (selectedTab) {
                0 -> {
                    ChaptersTab(
                        data = data,
                        onPlay = onPlay,
                        onDeleteFiles = vm::deleteFiles,
                        onDeleteCatalog = { confirmCatalog = true },
                        state = chaptersState,
                    )
                }

                1 -> {
                    SessionsTab(data = data, onPlay = onPlay, onDelete = vm::deleteBookmark, state = sessionsState)
                }

                else -> {
                    BookmarksTab(data = data, onPlay = onPlay, onDelete = vm::deleteBookmark, state = bookmarksState)
                }
            }
        }
    }

    if (confirmCatalog) {
        AlertDialog(
            onDismissRequest = { confirmCatalog = false },
            title = { Text("Удалить навсегда?") },
            text = { Text("Книга будет удалена из каталога вместе с прогрессом, закладками и привязкой к полкам. Необратимо.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmCatalog = false
                    vm.deleteFromCatalog()
                    onBack()
                }) {
                    Text("Удалить навсегда")
                }
            },
            dismissButton = { TextButton(onClick = { confirmCatalog = false }) { Text("Отмена") } },
        )
    }

    if (showBookmarkDialog) {
        var title by remember { mutableStateOf("Закладка") }
        AlertDialog(
            onDismissRequest = { showBookmarkDialog = false },
            title = { Text("Новая закладка") },
            text = {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название") })
            },
            confirmButton = {
                TextButton(onClick = {
                    val pos = ui?.progress?.positionMs ?: 0L
                    val chIdx = ui?.progress?.chapterIndex
                    vm.addBookmark(pos, title.ifBlank { "Закладка" }, chIdx)
                    showBookmarkDialog = false
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showBookmarkDialog = false }) { Text("Отмена") } },
        )
    }
}

/** Закреплённая шапка: обложка, статус, прогресс и кнопки управления. Описание — в вкладке «Главы». */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun BookHeader(
    data: BookDetailUi,
    onPlay: () -> Unit,
    onAddBookmark: () -> Unit,
    onSetStatus: (BookStatus) -> Unit,
) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (!data.book.filesPresent) {
            Surface(
                color = Missing.copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Файлы удалены с устройства — карточка сохранена",
                    style = MaterialTheme.typography.labelLarge,
                    color = Missing,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(10.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        // Текст слева, обложка справа — компактно и без пустоты по бокам.
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    data.book.title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                val cm =
                                    context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as
                                        android.content.ClipboardManager
                                cm.setPrimaryClip(
                                    android.content.ClipData.newPlainText("book-title", data.book.title),
                                )
                                android.widget.Toast
                                    .makeText(
                                        context,
                                        "Название скопировано",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                            },
                )
                data.book.author?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    statusLabel(data.book.status),
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor(data.book.status),
                )
                if (!data.book.filesPresent) {
                    Text(
                        "нет на устройстве",
                        style = MaterialTheme.typography.labelLarge,
                        color = Missing,
                    )
                }
                if (data.book.totalDurationMs > 0) {
                    Text(
                        formatDuration(data.book.totalDurationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                data.progress?.let { progress ->
                    val globalPos = globalProgressMs(data.chapters, progress)
                    val pct = progress.percent.coerceIn(0f, 1f)
                    Text(
                        "Прогресс: ${formatTime(globalPos)} / ${formatTime(data.book.totalDurationMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    LinearProgressIndicator(
                        progress = { pct },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(4.dp),
                    )
                }
            }
            BookCover(
                title = data.book.title,
                coverPath = data.book.coverPath,
                modifier =
                    Modifier
                        .size(width = 110.dp, height = 165.dp)
                        .shadow(8.dp, RoundedCornerShape(10.dp)),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onPlay,
                enabled = data.book.filesPresent,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    when {
                        !data.book.filesPresent -> "Файлы удалены"
                        data.progress != null -> "Продолжить"
                        else -> "Слушать"
                    },
                )
            }
            OutlinedButton(onClick = onAddBookmark) {
                Icon(Icons.Filled.Bookmark, contentDescription = "Закладка")
            }
        }
        // Ручной выбор статуса — показывается одинаково в Библиотеке, Полках и здесь.
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BookStatus.entries.forEach { s ->
                FilterChip(
                    selected = data.book.status == s,
                    onClick = { onSetStatus(s) },
                    label = {
                        Text(
                            statusLabel(s),
                            color =
                                if (data.book.status == s) {
                                    statusColor(s)
                                } else {
                                    Color.Unspecified
                                },
                        )
                    },
                )
            }
        }
    }
}

/** Вкладка «Главы»: описание, список глав, действия с книгой. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ChaptersTab(
    data: BookDetailUi,
    onPlay: (String, Int?, Long?) -> Unit,
    onDeleteFiles: () -> Unit,
    onDeleteCatalog: () -> Unit,
    state: LazyListState,
) {
    val currentChapter = data.progress?.chapterIndex ?: -1
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        data.book.description?.let { desc ->
            item {
                Text(desc, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Text("Главы (${data.chapters.size})", style = MaterialTheme.typography.titleMedium)
        }
        if (data.chapters.isEmpty()) {
            item {
                EmptyHint("Главы не найдены — файлы книги, возможно, удалены с устройства.")
            }
        } else {
            data.chapters.forEach { ch ->
                item(key = "ch-${ch.id}") {
                    ChapterRow(
                        chapter = ch,
                        isCurrent = ch.index == currentChapter,
                        onClick = { onPlay(data.book.id, ch.index, null) },
                    )
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDeleteFiles, modifier = Modifier.weight(1f)) {
                    Text(if (data.book.filesPresent) "Удалить с устройства" else "Уже удалено")
                }
                OutlinedButton(onClick = onDeleteCatalog, modifier = Modifier.weight(1f)) {
                    Text("Удалить из каталога")
                }
            }
        }
    }
}

/** Вкладка «Точки прослушивания»: авто-точки после пауз. Тап — продолжить с этого места. */
@Composable
private fun SessionsTab(
    data: BookDetailUi,
    onPlay: (String, Int?, Long?) -> Unit,
    onDelete: (String) -> Unit,
    state: LazyListState,
) {
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Точки прослушивания", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Сохраняются автоматически после паузы — место и дата, где вы остановились.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (data.sessions.isEmpty()) {
            item {
                EmptyHint("Точки появятся, когда поставишь книгу на паузу.")
            }
        } else {
            data.sessions.forEach { bm ->
                item(key = "sess-${bm.id}") {
                    BookmarkRow(
                        icon = { Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        title = bm.title,
                        subtitle = bookmarkSubtitle(data.chapters, bm),
                        onClick = { onPlay(data.book.id, bm.chapterIndex, bm.positionMs) },
                        onDelete = { onDelete(bm.id) },
                        deleteDescription = "Удалить точку",
                    )
                }
            }
        }
    }
}

/** Вкладка «Закладки»: ручные закладки. Тап — продолжить с этого места. */
@Composable
private fun BookmarksTab(
    data: BookDetailUi,
    onPlay: (String, Int?, Long?) -> Unit,
    onDelete: (String) -> Unit,
    state: LazyListState,
) {
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Закладки", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Ручные закладки, добавленные с плеера или кнопкой в шапке.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (data.bookmarks.isEmpty()) {
            item {
                EmptyHint("Закладок пока нет — добавь её кнопкой с закладкой в шапке или из плеера.")
            }
        } else {
            data.bookmarks.forEach { bm ->
                item(key = "bm-${bm.id}") {
                    BookmarkRow(
                        icon = { Icon(Icons.Filled.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        title = bm.title,
                        subtitle = bookmarkSubtitle(data.chapters, bm),
                        onClick = { onPlay(data.book.id, bm.chapterIndex, bm.positionMs) },
                        onDelete = { onDelete(bm.id) },
                        deleteDescription = "Удалить закладку",
                    )
                }
            }
        }
    }
}

/** Строка закладки/точки: иконка, название, позиция в книге + глава, удаление. */
@Composable
private fun BookmarkRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    deleteDescription: String,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            icon()
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = deleteDescription)
            }
        }
    }
}

/** Подпись закладки: глобальная позиция в книге + номер главы. */
private fun bookmarkSubtitle(
    chapters: List<Chapter>,
    bm: Bookmark,
): String =
    "${formatTime(globalBookmarkMs(chapters, bm))}" +
        (bm.chapterIndex?.let { " · глава ${it + 1}" } ?: "")

@Composable
private fun EmptyHint(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun ChapterRow(
    chapter: Chapter,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    Surface(
        onClick = onClick,
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            ) {
                Text(
                    text = "${chapter.index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    chapter.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (chapter.endMs - chapter.startMs > 0) {
                    Text(
                        formatDuration(chapter.endMs - chapter.startMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (isCurrent) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Глобальная (сквозная по книге) позиция прогресса для отображения на таймлайне. */
private fun globalProgressMs(
    chapters: List<Chapter>,
    progress: PlaybackProgress,
): Long {
    val idx = progress.chapterIndex.coerceIn(0, (chapters.size - 1).coerceAtLeast(0))
    val offset = chapters.subList(0, idx).sumOf { it.endMs.coerceAtLeast(0L) }
    return offset + progress.positionMs.coerceAtLeast(0L)
}

/** Глобальная позиция закладки/точки. Legacy-закладки (без главы) показывают как есть. */
private fun globalBookmarkMs(
    chapters: List<Chapter>,
    bm: Bookmark,
): Long {
    val idx = bm.chapterIndex ?: return bm.positionMs
    if (idx !in chapters.indices) return bm.positionMs
    val offset = chapters.subList(0, idx).sumOf { it.endMs.coerceAtLeast(0L) }
    return offset + bm.positionMs.coerceAtLeast(0L)
}

package com.nikit.audiobook.ui.library

import com.nikit.audiobook.domain.model.Book
import com.nikit.audiobook.domain.model.BookStatus
import com.nikit.audiobook.domain.model.PlaybackProgress

/** Чистая фильтрация/сортировка каталога — вынесена для тестируемости. */
fun applyLibraryFilters(
    books: List<Book>,
    progressByBook: Map<String, PlaybackProgress>,
    sort: LibrarySort,
    statusFilter: BookStatus?,
    onlyPresent: Boolean,
    query: String,
): List<Book> {
    val q = query.trim().lowercase()
    var filtered = books
    if (statusFilter != null) filtered = filtered.filter { it.status == statusFilter }
    if (onlyPresent) filtered = filtered.filter { it.filesPresent }
    if (q.isNotEmpty()) {
        filtered =
            filtered.filter {
                it.title.lowercase().contains(q) || (it.author ?: "").lowercase().contains(q)
            }
    }
    return when (sort) {
        LibrarySort.RECENT -> filtered.sortedByDescending { it.lastPlayedAt ?: it.addedAt }
        LibrarySort.TITLE -> filtered.sortedBy { it.title.lowercase() }
        LibrarySort.AUTHOR -> filtered.sortedBy { (it.author ?: "").lowercase() }
        LibrarySort.PROGRESS -> filtered.sortedByDescending { progressByBook[it.id]?.percent ?: 0f }
    }
}

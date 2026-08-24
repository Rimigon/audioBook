package com.nikit.audiobook.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikit.audiobook.data.repo.BookRepository
import com.nikit.audiobook.data.repo.ProgressRepository
import com.nikit.audiobook.data.saf.ScanSettings
import com.nikit.audiobook.domain.model.Book
import com.nikit.audiobook.domain.model.BookStatus
import com.nikit.audiobook.domain.model.PlaybackProgress
import com.nikit.audiobook.domain.usecase.DeleteBookFiles
import com.nikit.audiobook.domain.usecase.DeleteBookFromCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibrarySort { RECENT, TITLE, AUTHOR, PROGRESS }

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val bookRepository: BookRepository,
        private val progressRepository: ProgressRepository,
        private val scanSettings: ScanSettings,
        private val deleteBookFiles: DeleteBookFiles,
        private val deleteBookFromCatalog: DeleteBookFromCatalog,
    ) : ViewModel() {
        val sort = MutableStateFlow(LibrarySort.RECENT)
        val statusFilter = MutableStateFlow<BookStatus?>(null)
        val showOnlyPresent = MutableStateFlow(false)
        val searchQuery = MutableStateFlow("")

        @Suppress("UNCHECKED_CAST")
        val books: StateFlow<List<Book>> =
            combine(
                bookRepository.observeAll(),
                progressRepository.observeAll(),
                sort,
                statusFilter,
                showOnlyPresent,
                searchQuery,
            ) { values ->
                applyLibraryFilters(
                    books = values[0] as List<Book>,
                    progressByBook = values[1] as Map<String, PlaybackProgress>,
                    sort = values[2] as LibrarySort,
                    statusFilter = values[3] as BookStatus?,
                    onlyPresent = values[4] as Boolean,
                    query = values[5] as String,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val progress: StateFlow<Map<String, PlaybackProgress>> =
            progressRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

        /** Книги, скрытые из ряда «Продолжить». */
        val dismissedContinue: StateFlow<Set<String>> =
            scanSettings.dismissedContinue.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

        init {
            // Восстанавливаем последнюю открытую категорию после перезапуска.
            viewModelScope.launch {
                scanSettings.lastCategory.first()?.let { name ->
                    runCatching { BookStatus.valueOf(name) }.getOrNull()?.let { statusFilter.value = it }
                }
            }
        }

        fun dismissFromContinue(id: String) = viewModelScope.launch { scanSettings.dismissFromContinue(id) }

        /** Массовое удаление: файлы с устройства (карточки остаются). */
        fun deleteFiles(ids: List<String>) =
            viewModelScope.launch {
                ids.forEach { runCatching { deleteBookFiles(it) } }
            }

        /** Массовое удаление: книги навсегда из каталога. */
        fun deleteFromCatalog(ids: List<String>) =
            viewModelScope.launch {
                ids.forEach { runCatching { deleteBookFromCatalog(it) } }
            }

        fun setStatusFilter(s: BookStatus?) {
            statusFilter.value = s
            viewModelScope.launch { scanSettings.setLastCategory(s?.name) }
        }

        fun setSort(s: LibrarySort) {
            sort.value = s
        }

        fun toggleOnlyPresent() {
            showOnlyPresent.value = !showOnlyPresent.value
        }

        fun setSearchQuery(q: String) {
            searchQuery.value = q
        }
    }

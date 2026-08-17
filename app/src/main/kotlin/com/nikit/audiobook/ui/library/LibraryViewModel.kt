package com.nikit.audiobook.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikit.audiobook.data.repo.BookRepository
import com.nikit.audiobook.data.repo.ProgressRepository
import com.nikit.audiobook.domain.model.Book
import com.nikit.audiobook.domain.model.BookStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class LibrarySort { RECENT, TITLE, AUTHOR, PROGRESS }

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val bookRepository: BookRepository,
        private val progressRepository: ProgressRepository,
    ) : ViewModel() {
        val sort = MutableStateFlow(LibrarySort.RECENT)
        val statusFilter = MutableStateFlow<BookStatus?>(null)
        val showOnlyPresent = MutableStateFlow(false)

        val books: StateFlow<List<Book>> =
            combine(
                bookRepository.observeAll(),
                progressRepository.observeAll(),
                sort,
                statusFilter,
                showOnlyPresent,
            ) { list, progress, s, f, present ->
                applyLibraryFilters(list, progress.associateBy { it.bookId }, s, f, present, "")
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        fun setSort(s: LibrarySort) {
            sort.value = s
        }

        fun setStatusFilter(s: BookStatus?) {
            statusFilter.value = s
        }

        fun toggleOnlyPresent() {
            showOnlyPresent.value = !showOnlyPresent.value
        }
    }

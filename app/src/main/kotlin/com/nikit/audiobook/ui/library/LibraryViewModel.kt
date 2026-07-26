package com.nikit.audiobook.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikit.audiobook.data.repo.BookRepository
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
    ) : ViewModel() {
        val sort = MutableStateFlow(LibrarySort.RECENT)
        val statusFilter = MutableStateFlow<BookStatus?>(null)
        val showOnlyPresent = MutableStateFlow(false)

        val books: StateFlow<List<Book>> =
            combine(bookRepository.observeAll(), sort, statusFilter, showOnlyPresent) { list, s, f, present ->
                var filtered = list
                if (f != null) filtered = filtered.filter { it.status == f }
                if (present) filtered = filtered.filter { it.filesPresent }
                when (s) {
                    LibrarySort.RECENT -> filtered.sortedByDescending { it.lastPlayedAt ?: it.addedAt }
                    LibrarySort.TITLE -> filtered.sortedBy { it.title.lowercase() }
                    LibrarySort.AUTHOR -> filtered.sortedBy { (it.author ?: "").lowercase() }
                    LibrarySort.PROGRESS -> filtered.sortedByDescending { it.status == BookStatus.READING }
                }
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

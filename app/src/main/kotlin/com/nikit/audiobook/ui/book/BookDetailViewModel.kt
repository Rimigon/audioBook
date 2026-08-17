package com.nikit.audiobook.ui.book

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikit.audiobook.data.repo.BookRepository
import com.nikit.audiobook.data.repo.BookmarkRepository
import com.nikit.audiobook.data.repo.ChapterRepository
import com.nikit.audiobook.data.repo.ProgressRepository
import com.nikit.audiobook.data.repo.ShelfRepository
import com.nikit.audiobook.data.repo.TagRepository
import com.nikit.audiobook.domain.model.Bookmark
import com.nikit.audiobook.domain.usecase.DeleteBookFiles
import com.nikit.audiobook.domain.usecase.DeleteBookFromCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookDetailViewModel
    @Inject
    constructor(
        private val bookRepository: BookRepository,
        private val chapterRepository: ChapterRepository,
        private val bookmarkRepository: BookmarkRepository,
        private val progressRepository: ProgressRepository,
        private val shelfRepository: ShelfRepository,
        private val tagRepository: TagRepository,
        private val deleteBookFiles: DeleteBookFiles,
        private val deleteBookFromCatalog: DeleteBookFromCatalog,
        @ApplicationContext private val context: Context,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val bookId: String = checkNotNull(savedStateHandle["bookId"])

        val state: StateFlow<BookDetailUi?> =
            combine(
                bookRepository.observeBook(bookId),
                chapterRepository.observeByBook(bookId),
                bookmarkRepository.observeByBook(bookId),
                progressRepository.observeByBook(bookId),
            ) { book, chapters, bookmarks, progress ->
                if (book == null) null else BookDetailUi(book, chapters, bookmarks, progress)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        val shelves =
            shelfRepository
                .observeAll()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        fun deleteFiles() =
            viewModelScope.launch {
                if (!deleteBookFiles(bookId)) {
                    Toast.makeText(context, "Не удалось удалить файлы с устройства", Toast.LENGTH_LONG).show()
                }
            }

        fun deleteFromCatalog() = viewModelScope.launch { deleteBookFromCatalog(bookId) }

        fun addBookmark(
            positionMs: Long,
            title: String,
        ) = viewModelScope.launch {
            bookmarkRepository.add(Bookmark(bookId = bookId, positionMs = positionMs, title = title))
        }

        fun deleteBookmark(id: String) = viewModelScope.launch { bookmarkRepository.delete(id) }

        fun toggleShelf(shelfId: String) =
            viewModelScope.launch {
                shelfRepository.addBook(shelfId, bookId)
            }
    }

package com.nikit.audiobook.ui.book

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikit.audiobook.data.diag.CrashLogger
import com.nikit.audiobook.data.repo.BookRepository
import com.nikit.audiobook.data.repo.BookmarkRepository
import com.nikit.audiobook.data.repo.ChapterRepository
import com.nikit.audiobook.data.repo.ProgressRepository
import com.nikit.audiobook.domain.model.BookStatus
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
        private val deleteBookFiles: DeleteBookFiles,
        private val deleteBookFromCatalog: DeleteBookFromCatalog,
        @ApplicationContext private val context: Context,
        private val crashLogger: CrashLogger,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val bookId: String = checkNotNull(savedStateHandle["bookId"])

        val state: StateFlow<BookDetailUi?> =
            combine(
                bookRepository.observeBook(bookId),
                chapterRepository.observeByBook(bookId),
                bookmarkRepository.observeManualByBook(bookId),
                bookmarkRepository.observeSessionsByBook(bookId),
                progressRepository.observeByBook(bookId),
            ) { book, chapters, bookmarks, sessions, progress ->
                if (book == null) null else BookDetailUi(book, chapters, bookmarks, sessions, progress)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        fun deleteFiles() =
            viewModelScope.launch {
                if (!deleteBookFiles(bookId)) {
                    // Показываем реальную причину (она пишется в Crash-лог при сбое),
                    // а не общий текст — иначе не понять, SecurityException это или битый URI.
                    val reason =
                        crashLogger
                            .logs()
                            .firstOrNull()
                            ?.let { logFile ->
                                crashLogger
                                    .read(logFile)
                                    .lineSequence()
                                    .firstOrNull { "Удаление файла не удалось" in it }
                            }
                    Toast
                        .makeText(
                            context,
                            reason ?: "Не удалось удалить файлы с устройства",
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }

        fun deleteFromCatalog() = viewModelScope.launch { deleteBookFromCatalog(bookId) }

        fun addBookmark(
            positionMs: Long,
            title: String,
            chapterIndex: Int? = null,
        ) = viewModelScope.launch {
            bookmarkRepository.add(
                Bookmark(
                    bookId = bookId,
                    positionMs = positionMs,
                    title = title,
                    chapterIndex = chapterIndex,
                ),
            )
        }

        fun deleteBookmark(id: String) = viewModelScope.launch { bookmarkRepository.delete(id) }

        /** Ручная установка статуса (Читаю/Прочитал/Хочу/Брошено/Пауза). */
        fun setStatus(status: BookStatus) =
            viewModelScope.launch {
                bookRepository.updateStatus(bookId, status)
            }
    }

package com.nikit.audiobook.domain.usecase

import com.nikit.audiobook.data.repo.BookRepository
import com.nikit.audiobook.data.repo.ChapterRepository
import com.nikit.audiobook.data.saf.FileDeleter
import com.nikit.audiobook.domain.model.Book
import com.nikit.audiobook.domain.model.Chapter
import com.nikit.audiobook.domain.model.FileType
import javax.inject.Inject

class DeleteBookFiles
    @Inject
    constructor(
        private val repo: BookRepository,
        private val chapterRepository: ChapterRepository,
        private val fileDeleter: FileDeleter,
    ) {
        /**
         * Удаляет аудиофайлы с устройства, карточку сохраняет.
         * @return false, если файлы удалить не удалось (БД в этом случае не помечается).
         */
        suspend operator fun invoke(bookId: String): Boolean {
            val book = repo.getBook(bookId) ?: return false
            val ok = fileDeleter.delete(targetUris(book, chapterRepository.getByBook(bookId)))
            if (ok) repo.markFilesDeleted(bookId)
            return ok
        }

        private fun targetUris(
            book: Book,
            chapters: List<Chapter>,
        ): List<String> {
            val uris = chapters.mapNotNull { it.filePath }.toMutableList()
            // папка книги удаляется рекурсивно (вместе с обложками/txt); файлы глав — запасной вариант
            if (book.fileType == FileType.FOLDER && book.sourceUri?.contains("/document/") == true) {
                uris.add(0, book.sourceUri)
            }
            return uris
        }
    }

package com.nikit.audiobook.data.repo

import com.nikit.audiobook.data.db.dao.BookDao
import com.nikit.audiobook.data.db.dao.ChapterDao
import com.nikit.audiobook.domain.model.Book
import com.nikit.audiobook.domain.model.Chapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository
    @Inject
    constructor(
        private val bookDao: BookDao,
        private val chapterDao: ChapterDao,
    ) {
        fun observeAll(): Flow<List<Book>> = bookDao.observeAll().map { list -> list.map { it.toDomain() } }

        fun observeBook(id: String): Flow<Book?> = bookDao.observeById(id).map { it?.toDomain() }

        suspend fun getBook(id: String): Book? = bookDao.getById(id)?.toDomain()

        suspend fun getBookBySourceUri(uri: String): Book? = bookDao.getBySourceUri(uri)?.toDomain()

        suspend fun upsert(
            book: Book,
            chapters: List<Chapter> = emptyList(),
        ) {
            bookDao.upsert(book.toEntity())
            if (chapters.isNotEmpty()) chapterDao.insertAll(chapters.map { it.toEntity() })
        }

        /** Удалить аудиофайлы с устройства, карточку сохранить. */
        suspend fun markFilesDeleted(bookId: String) {
            bookDao.markFilesDeleted(bookId)
            chapterDao.clearFilePathsForBook(bookId)
        }

        /** Начало/продолжение прослушивания: актуализировать lastPlayedAt, статус — READING (дослушанные не сбрасываются). */
        suspend fun markPlayed(bookId: String) {
            bookDao.markPlayed(bookId, System.currentTimeMillis())
        }

        /** Книга дослушана до конца. */
        suspend fun markCompleted(bookId: String) {
            bookDao.markCompleted(bookId, System.currentTimeMillis())
        }

        /** Удалить книгу из каталога навсегда (каскад чистит главы/закладки/прогресс/полки/теги). */
        suspend fun deleteBookPermanently(bookId: String) {
            bookDao.deleteById(bookId)
        }
    }

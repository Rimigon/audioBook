package com.nikit.audiobook.data.repo

import com.nikit.audiobook.data.db.dao.BookmarkDao
import com.nikit.audiobook.domain.model.Bookmark
import com.nikit.audiobook.domain.model.BookmarkKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepository
    @Inject
    constructor(
        private val dao: BookmarkDao,
    ) {
        fun observeManualByBook(bookId: String): Flow<List<Bookmark>> =
            dao.observeByBook(bookId, BookmarkKind.MANUAL).map { list -> list.map { it.toDomain() } }

        fun observeSessionsByBook(bookId: String): Flow<List<Bookmark>> =
            dao.observeSessionsByBook(bookId).map { list -> list.map { it.toDomain() } }

        suspend fun add(bookmark: Bookmark) = dao.upsert(bookmark.toEntity())

        suspend fun delete(id: String) = dao.deleteById(id)
    }

package com.nikit.audiobook.data.repo

import com.nikit.audiobook.data.db.dao.BookmarkDao
import com.nikit.audiobook.domain.model.Bookmark
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
        fun observeByBook(bookId: String): Flow<List<Bookmark>> = dao.observeByBook(bookId).map { it.map { e -> e.toDomain() } }

        suspend fun add(bookmark: Bookmark) = dao.upsert(bookmark.toEntity())

        suspend fun delete(id: String) = dao.deleteById(id)
    }

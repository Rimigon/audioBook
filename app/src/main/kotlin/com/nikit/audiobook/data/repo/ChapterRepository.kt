package com.nikit.audiobook.data.repo

import com.nikit.audiobook.data.db.dao.ChapterDao
import com.nikit.audiobook.domain.model.Chapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterRepository
    @Inject
    constructor(
        private val dao: ChapterDao,
    ) {
        fun observeByBook(bookId: String): Flow<List<Chapter>> = dao.observeByBook(bookId).map { it.map { e -> e.toDomain() } }

        suspend fun getByBook(bookId: String): List<Chapter> = dao.getByBook(bookId).map { it.toDomain() }

        suspend fun deleteByBook(bookId: String) {
            dao.deleteByBook(bookId)
        }
    }

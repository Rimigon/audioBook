package com.nikit.audiobook.data.repo

import com.nikit.audiobook.data.db.dao.PlaybackProgressDao
import com.nikit.audiobook.domain.model.PlaybackProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepository
    @Inject
    constructor(
        private val dao: PlaybackProgressDao,
    ) {
        fun observeByBook(bookId: String): Flow<PlaybackProgress?> = dao.observeByBook(bookId).map { it?.toDomain() }

        fun observeAll(): Flow<Map<String, PlaybackProgress>> =
            dao.observeAll().map { list -> list.associateBy({ it.bookId }, { it.toDomain() }) }

        suspend fun get(bookId: String): PlaybackProgress? = dao.getByBook(bookId)?.toDomain()

        suspend fun upsert(progress: PlaybackProgress) = dao.upsert(progress.toEntity())
    }

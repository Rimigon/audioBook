package com.nikit.audiobook.data.repo

import com.nikit.audiobook.data.db.dao.TagDao
import com.nikit.audiobook.data.db.entity.BookTagEntity
import com.nikit.audiobook.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository
    @Inject
    constructor(
        private val dao: TagDao,
    ) {
        fun observeAll(): Flow<List<Tag>> = dao.observeAll().map { it.map { e -> e.toDomain() } }

        fun observeTagsOfBook(bookId: String): Flow<List<Tag>> = dao.observeTagsOfBook(bookId).map { it.map { e -> e.toDomain() } }

        suspend fun upsert(tag: Tag) = dao.upsert(tag.toEntity())

        suspend fun link(
            tagId: String,
            bookId: String,
        ) = dao.link(BookTagEntity(tagId, bookId))

        suspend fun unlink(
            tagId: String,
            bookId: String,
        ) = dao.unlink(tagId, bookId)
    }

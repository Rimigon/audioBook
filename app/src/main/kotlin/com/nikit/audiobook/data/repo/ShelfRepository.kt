package com.nikit.audiobook.data.repo

import com.nikit.audiobook.data.db.dao.ShelfDao
import com.nikit.audiobook.data.db.entity.ShelfMembershipEntity
import com.nikit.audiobook.domain.model.Book
import com.nikit.audiobook.domain.model.Shelf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShelfRepository
    @Inject
    constructor(
        private val dao: ShelfDao,
    ) {
        fun observeAll(): Flow<List<Shelf>> = dao.observeAll().map { it.map { e -> e.toDomain() } }

        suspend fun upsert(shelf: Shelf) = dao.upsert(shelf.toEntity())

        suspend fun addBook(
            shelfId: String,
            bookId: String,
        ) = dao.addMembership(ShelfMembershipEntity(shelfId, bookId))

        suspend fun removeBook(
            shelfId: String,
            bookId: String,
        ) = dao.removeMembership(shelfId, bookId)

        fun observeBooksOfShelf(shelfId: String): Flow<List<Book>> = dao.observeBooksOfShelf(shelfId).map { it.map { e -> e.toDomain() } }
    }

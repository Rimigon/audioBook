package com.nikit.audiobook.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nikit.audiobook.data.db.entity.BookEntity
import com.nikit.audiobook.data.db.entity.ShelfEntity
import com.nikit.audiobook.data.db.entity.ShelfMembershipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShelfDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(shelf: ShelfEntity)

    @Query("SELECT * FROM shelves ORDER BY sortIndex ASC")
    fun observeAll(): Flow<List<ShelfEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMembership(m: ShelfMembershipEntity)

    @Query("DELETE FROM shelf_memberships WHERE shelfId = :shelfId AND bookId = :bookId")
    suspend fun removeMembership(
        shelfId: String,
        bookId: String,
    )

    @Query(
        "SELECT * FROM books WHERE id IN " +
            "(SELECT bookId FROM shelf_memberships WHERE shelfId = :shelfId) " +
            "ORDER BY lastPlayedAt DESC",
    )
    fun observeBooksOfShelf(shelfId: String): Flow<List<BookEntity>>
}

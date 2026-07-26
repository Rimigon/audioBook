package com.nikit.audiobook.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nikit.audiobook.data.db.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bookmark: BookmarkEntity)

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY positionMs ASC")
    fun observeByBook(bookId: String): Flow<List<BookmarkEntity>>

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: String)
}

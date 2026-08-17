package com.nikit.audiobook.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nikit.audiobook.data.db.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(book: BookEntity)

    @Query("SELECT * FROM books ORDER BY lastPlayedAt DESC, addedAt DESC")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeById(id: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: String): BookEntity?

    @Query("SELECT * FROM books WHERE sourceUri = :uri LIMIT 1")
    suspend fun getBySourceUri(uri: String): BookEntity?

    @Query("UPDATE books SET filesPresent = 0, sourceUri = NULL WHERE id = :id")
    suspend fun markFilesDeleted(id: String)

    @Query("UPDATE books SET status = CASE WHEN status = 'COMPLETED' THEN status ELSE 'READING' END, lastPlayedAt = :now WHERE id = :id")
    suspend fun markPlayed(
        id: String,
        now: Long,
    )

    @Query("UPDATE books SET status = 'COMPLETED', completedAt = :now, lastPlayedAt = :now WHERE id = :id")
    suspend fun markCompleted(
        id: String,
        now: Long,
    )

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteById(id: String)
}

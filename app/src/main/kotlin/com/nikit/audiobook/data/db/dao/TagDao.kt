package com.nikit.audiobook.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nikit.audiobook.data.db.entity.BookTagEntity
import com.nikit.audiobook.data.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tag: TagEntity)

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun link(link: BookTagEntity)

    @Query("DELETE FROM book_tags WHERE tagId = :tagId AND bookId = :bookId")
    suspend fun unlink(
        tagId: String,
        bookId: String,
    )

    @Query(
        "SELECT * FROM tags WHERE id IN " +
            "(SELECT tagId FROM book_tags WHERE bookId = :bookId) ORDER BY name ASC",
    )
    fun observeTagsOfBook(bookId: String): Flow<List<TagEntity>>
}

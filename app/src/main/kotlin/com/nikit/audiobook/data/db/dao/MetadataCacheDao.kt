package com.nikit.audiobook.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nikit.audiobook.data.db.entity.MetadataCacheEntity

@Dao
interface MetadataCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: MetadataCacheEntity)

    @Query("SELECT * FROM metadata_cache WHERE queryKey = :queryKey LIMIT 1")
    suspend fun findByQueryKey(queryKey: String): MetadataCacheEntity?

    @Query("DELETE FROM metadata_cache WHERE id = :id")
    suspend fun deleteById(id: String)
}

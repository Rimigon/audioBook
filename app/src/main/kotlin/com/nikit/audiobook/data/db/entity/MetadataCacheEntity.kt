package com.nikit.audiobook.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "metadata_cache")
data class MetadataCacheEntity(
    @PrimaryKey val id: String,
    val queryKey: String,
    val payloadJson: String,
    val source: String,
    val createdAt: Long,
)

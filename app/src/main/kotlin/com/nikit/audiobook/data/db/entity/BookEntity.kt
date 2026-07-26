package com.nikit.audiobook.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nikit.audiobook.domain.model.BookStatus
import com.nikit.audiobook.domain.model.FileType
import com.nikit.audiobook.domain.model.SourceKind

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val series: String?,
    val seriesIndex: Int?,
    val genre: String?,
    val description: String?,
    val year: Int?,
    val coverPath: String?,
    val sourceUri: String?,
    val filesPresent: Boolean,
    val fileType: FileType,
    val totalDurationMs: Long,
    val addedAt: Long,
    val lastPlayedAt: Long?,
    val completedAt: Long?,
    val rating: Int,
    val review: String?,
    val status: BookStatus,
    val sourceKind: SourceKind,
    val originalPath: String?,
    val manuallyEdited: Boolean,
)

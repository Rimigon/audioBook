package com.nikit.audiobook.domain.model

import java.util.UUID

data class Book(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val author: String? = null,
    val series: String? = null,
    val seriesIndex: Int? = null,
    val genre: String? = null,
    val description: String? = null,
    val year: Int? = null,
    val coverPath: String? = null,
    val sourceUri: String? = null,
    val filesPresent: Boolean = true,
    val fileType: FileType = FileType.SINGLE_FILE,
    val totalDurationMs: Long = 0L,
    val addedAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long? = null,
    val completedAt: Long? = null,
    val rating: Int = 0,
    val review: String? = null,
    val status: BookStatus = BookStatus.WISHLIST,
    val sourceKind: SourceKind = SourceKind.LOCAL_FILE,
    val originalPath: String? = null,
    val manuallyEdited: Boolean = false,
)

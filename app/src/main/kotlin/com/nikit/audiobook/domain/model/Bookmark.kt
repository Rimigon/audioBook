package com.nikit.audiobook.domain.model

import java.util.UUID

data class Bookmark(
    val id: String = UUID.randomUUID().toString(),
    val bookId: String,
    val positionMs: Long,
    val title: String,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

package com.nikit.audiobook.domain.model

import java.util.UUID

data class Chapter(
    val id: String = UUID.randomUUID().toString(),
    val bookId: String,
    val index: Int,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val filePath: String? = null,
)

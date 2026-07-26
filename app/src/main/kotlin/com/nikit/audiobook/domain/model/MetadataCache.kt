package com.nikit.audiobook.domain.model

import java.util.UUID

data class MetadataCache(
    val id: String = UUID.randomUUID().toString(),
    val queryKey: String,
    val payloadJson: String,
    val source: String,
    val createdAt: Long = System.currentTimeMillis(),
)

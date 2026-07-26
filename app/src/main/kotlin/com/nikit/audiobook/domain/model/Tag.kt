package com.nikit.audiobook.domain.model

import java.util.UUID

data class Tag(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
)

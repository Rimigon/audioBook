package com.nikit.audiobook.domain.model

import java.util.UUID

data class Shelf(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String? = null,
    val sortIndex: Int = 0,
)

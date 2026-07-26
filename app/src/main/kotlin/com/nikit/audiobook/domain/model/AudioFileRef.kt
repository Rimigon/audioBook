package com.nikit.audiobook.domain.model

data class AudioFileRef(
    val uri: String,
    val name: String,
    val mime: String? = null,
)

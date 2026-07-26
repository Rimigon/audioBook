package com.nikit.audiobook.domain.model

data class PlaybackProgress(
    val bookId: String,
    val positionMs: Long = 0L,
    val chapterIndex: Int = 0,
    val percent: Float = 0f,
    val lastPlayedAt: Long = System.currentTimeMillis(),
)

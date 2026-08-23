package com.nikit.audiobook.ui.book

import com.nikit.audiobook.domain.model.Book
import com.nikit.audiobook.domain.model.Bookmark
import com.nikit.audiobook.domain.model.Chapter
import com.nikit.audiobook.domain.model.PlaybackProgress

data class BookDetailUi(
    val book: Book,
    val chapters: List<Chapter>,
    val bookmarks: List<Bookmark>,
    val sessions: List<Bookmark>,
    val progress: PlaybackProgress?,
)

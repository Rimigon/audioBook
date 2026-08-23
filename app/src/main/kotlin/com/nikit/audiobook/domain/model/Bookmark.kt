package com.nikit.audiobook.domain.model

import java.util.UUID

/** Тип закладки. MANUAL — создана пользователем, SESSION — авто-точка по окончании сессии прослушивания. */
object BookmarkKind {
    const val MANUAL = 0
    const val SESSION = 1
}

data class Bookmark(
    val id: String = UUID.randomUUID().toString(),
    val bookId: String,
    val positionMs: Long,
    val title: String,
    val note: String? = null,
    val kind: Int = BookmarkKind.MANUAL,
    /** Индекс главы, к которому относится позиция (для новых закладок/точек). null — legacy. */
    val chapterIndex: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

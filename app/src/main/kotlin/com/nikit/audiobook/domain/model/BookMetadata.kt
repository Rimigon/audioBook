package com.nikit.audiobook.domain.model

/** Метаданные, прочитанные из тегов файла. */
data class BookMetadata(
    val title: String? = null,
    val author: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val description: String? = null,
    val coverBytes: ByteArray? = null,
    val durationMs: Long = 0L,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BookMetadata) return false
        return title == other.title && author == other.author && album == other.album &&
            genre == other.genre && year == other.year && description == other.description &&
            durationMs == other.durationMs
        // coverBytes намеренно не участвует в equals (массив)
    }

    override fun hashCode(): Int {
        var r = title?.hashCode() ?: 0
        r = 31 * r + (author?.hashCode() ?: 0)
        r = 31 * r + (album?.hashCode() ?: 0)
        r = 31 * r + (genre?.hashCode() ?: 0)
        r = 31 * r + (year ?: 0)
        r = 31 * r + (description?.hashCode() ?: 0)
        r = 31 * r + durationMs.hashCode()
        return r
    }
}

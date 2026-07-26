package com.nikit.audiobook.metadata.online

/** Результат онлайн-поиска метаданных. */
data class MetadataResult(
    val title: String,
    val author: String? = null,
    val description: String? = null,
    val coverUrl: String? = null,
    val source: String,
)

/** Источник онлайн-метаданных. Реализации парсят JSON-ответы. */
interface OnlineMetadataSource {
    val name: String

    suspend fun search(
        title: String,
        author: String?,
    ): MetadataResult?
}

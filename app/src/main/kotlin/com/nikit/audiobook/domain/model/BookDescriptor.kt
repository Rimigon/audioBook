package com.nikit.audiobook.domain.model

/** Результат классификации структуры папок: кандидат на книгу. */
data class BookDescriptor(
    val title: String,
    val type: FileType,
    val files: List<AudioFileRef>,
    val sourceUri: String,
    val sourceKind: SourceKind,
)

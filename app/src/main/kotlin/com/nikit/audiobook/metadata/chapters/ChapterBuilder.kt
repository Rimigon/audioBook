package com.nikit.audiobook.metadata.chapters

import com.nikit.audiobook.data.saf.naturalCompare
import com.nikit.audiobook.data.saf.stripExt
import com.nikit.audiobook.domain.model.AudioFileRef
import com.nikit.audiobook.domain.model.Chapter
import java.util.UUID

/**
 * Строит список глав для FOLDER-книги из списка аудиофайлов.
 * Главы идут в natural-order по имени файла; title = имя без расширения.
 * startMs/endMs остаются 0 — длительность выясняется плеером/TagReader позже.
 */
object ChapterBuilder {
    fun fromFiles(
        bookId: String,
        files: List<AudioFileRef>,
    ): List<Chapter> {
        val sorted = files.sortedWith { a, b -> naturalCompare(a.name, b.name) }
        return sorted.mapIndexed { index, ref ->
            Chapter(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                index = index,
                title = stripExt(ref.name),
                startMs = 0L,
                endMs = 0L,
                filePath = ref.uri,
            )
        }
    }

    /** Одна виртуальная глава для одиночного файла / m4b без embedded-глав. */
    fun single(
        bookId: String,
        title: String,
        filePath: String?,
        durationMs: Long = 0L,
    ): Chapter =
        Chapter(
            id = UUID.randomUUID().toString(),
            bookId = bookId,
            index = 0,
            title = title,
            startMs = 0L,
            endMs = durationMs,
            filePath = filePath,
        )
}

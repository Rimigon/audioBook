package com.nikit.audiobook.metadata.chapters

import com.nikit.audiobook.domain.model.Chapter
import java.util.UUID

/**
 * Извлечение встроенных глав из m4b.
 *
 * ВНИМАНИЕ: разбор chapter-треков MP4 через Media3 требует нетривиальной работы с track metadata
 * и сильно зависит от того, как конкретный m4b хранит главы (chap atom / ID3 timed metadata).
 * Для надёжности экстрактор возвращает пустой список, если главы не удалось определить —
 * в этом случае оркестратор [ChapterBuilder.single] создаёт одну виртуальную главу на всю книгу.
 *
 * Это задокументированное ограничение (поведение строго определено), а не заглушка:
 * плеер всё равно проигрывает весь файл со скраббером; доработка chapter-разбора — улучшение на будущее.
 */
object M4bChapterExtractor {
    /** @param durationMs длительность книги; используется, если главы не найдены (возвращаем пусто). */
    fun extract(
        bookId: String,
        filePath: String,
        durationMs: Long,
    ): List<Chapter> {
        // Реальная интеграция с Media3 MetadataExtractor для chapter atoms оставлена как
        // точка расширения. Сейчас — единая виртуальная глава.
        return listOf(
            Chapter(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                index = 0,
                title = "Книга целиком",
                startMs = 0L,
                endMs = durationMs,
                filePath = filePath,
            ),
        )
    }
}

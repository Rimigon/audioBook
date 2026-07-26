package com.nikit.audiobook.data.saf

import android.net.Uri
import com.nikit.audiobook.data.repo.BookRepository
import com.nikit.audiobook.domain.model.Book
import com.nikit.audiobook.domain.model.BookDescriptor
import com.nikit.audiobook.domain.model.BookStatus
import com.nikit.audiobook.domain.model.Chapter
import com.nikit.audiobook.domain.model.FileType
import com.nikit.audiobook.metadata.chapters.ChapterBuilder
import com.nikit.audiobook.metadata.chapters.M4bChapterExtractor
import com.nikit.audiobook.metadata.online.MetadataEnricher
import com.nikit.audiobook.metadata.tags.TagReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Связка: SAF-обход → классификация → чтение тегов → построение глав → онлайн-обогащение → репозиторий.
 * Дедуп по [BookDescriptor.sourceUri]: книгу с уже существующим sourceUri не пересоздаём.
 * Существующие книги с manuallyEdited=true не перетираются онлайн-обогащением (см. enrichBook).
 */
@Singleton
class ScanFacade
    @Inject
    constructor(
        private val bookRepository: BookRepository,
        private val folderScanner: FolderScanner,
        private val tagReader: TagReader,
        private val enricher: MetadataEnricher,
        private val coverStore: com.nikit.audiobook.data.cover.CoverStore,
    ) {
        /** Сканирует папку [treeUri] и импортирует новые книги. */
        suspend fun scanNow(treeUri: Uri): Int {
            val nodes = folderScanner.buildTree(treeUri)
            val descriptors = BookClassifier.classify(nodes)
            return importDescriptors(descriptors)
        }

        /** Импортирует список дескрипторов. Возвращает количество добавленных книг. */
        suspend fun importDescriptors(descriptors: List<BookDescriptor>): Int {
            var added = 0
            for (d in descriptors) {
                if (bookRepository.getBookBySourceUri(d.sourceUri) != null) continue
                importOne(d)
                added++
            }
            return added
        }

        private suspend fun importOne(d: BookDescriptor) {
            val realId =
                java.util.UUID
                    .randomUUID()
                    .toString()
            val chapters = buildChapters(realId, d)
            val meta =
                d.files.firstOrNull()?.let { tagReader.read(it.uri) } ?: com.nikit.audiobook.domain.model
                    .BookMetadata()
            var title = d.title
            var author = meta.author
            var description: String? = meta.description ?: meta.album
            var cover: String? = meta.coverBytes?.let { coverStore.saveBytes(it, d.sourceUri) }
            var genre = meta.genre
            var year = meta.year

            // Онлайн-обогащение, если теги неполные (нет автора или нет описания/обложки)
            if (author.isNullOrBlank() || description.isNullOrBlank() || cover == null) {
                val online = enricher.enrich(title, author)
                if (online != null) {
                    if (title.isBlank()) title = online.title
                    if (author.isNullOrBlank()) author = online.author
                    if (description.isNullOrBlank()) description = online.description
                    if (cover == null && !online.coverUrl.isNullOrBlank()) {
                        cover = coverStore.download(online.coverUrl, d.sourceUri)
                    }
                }
            }

            val totalDurationMs = chapters.sumOf { it.endMs }
            val book =
                Book(
                    id = realId,
                    title = title,
                    author = author,
                    description = description,
                    genre = genre,
                    year = year,
                    coverPath = cover,
                    sourceUri = d.sourceUri,
                    filesPresent = true,
                    fileType = d.type,
                    totalDurationMs = totalDurationMs,
                    status = BookStatus.READING,
                    sourceKind = d.sourceKind,
                    originalPath = null,
                    manuallyEdited = false,
                )
            bookRepository.upsert(book, chapters)
        }

        private fun buildChapters(
            bookId: String,
            d: BookDescriptor,
        ): List<Chapter> =
            when (d.type) {
                FileType.FOLDER -> {
                    val built = ChapterBuilder.fromFiles(bookId, d.files)
                    // заполнить длительность каждой главы реальным чтением тегов
                    built.mapIndexed { idx, ch ->
                        val dur = tagReader.read(d.files[idx].uri).durationMs
                        ch.copy(endMs = dur)
                    }
                }

                FileType.M4B -> {
                    val dur = tagReader.read(d.files.first().uri).durationMs
                    M4bChapterExtractor.extract(bookId, d.files.first().uri, dur)
                }

                FileType.SINGLE_FILE -> {
                    val dur = tagReader.read(d.files.first().uri).durationMs
                    listOf(ChapterBuilder.single(bookId, d.title, d.files.first().uri, dur))
                }
            }
    }

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
import com.nikit.audiobook.player.controller.PlayerSettings
import dagger.hilt.android.qualifiers.ApplicationContext
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
        @ApplicationContext private val context: android.content.Context,
    ) {
        /** Сканирует папку [treeUri] и импортирует новые книги. */
        suspend fun scanNow(treeUri: Uri): ScanResult {
            val nodes =
                runCatching { folderScanner.buildTree(treeUri) }.getOrElse {
                    return ScanResult(found = 0, added = 0, failures = listOf("buildTree: ${it.message}"))
                }
            val descriptors = BookClassifier.classify(nodes)
            val r = importDescriptors(descriptors)
            // Старые сборки помечали все импортированные книги как READING ошибочно —
            // возвращаем WISHLIST тем, у кого нет реального прогресса прослушивания.
            bookRepository.normalizeStatuses()
            return r
        }

        /** Импортирует список дескрипторов. Возвращает итог: добавлено/пропущено/ошибки. */
        suspend fun importDescriptors(descriptors: List<BookDescriptor>): ScanResult {
            var added = 0
            var skipped = 0
            val failures = mutableListOf<String>()
            for (d in descriptors) {
                try {
                    if (bookRepository.getBookBySourceUri(d.sourceUri) != null) {
                        skipped++
                        continue
                    }
                    // Одна книга не должна валить весь скан (сбой сети/обогащения/обложки).
                    runCatching { importOne(d) }
                        .onSuccess { added++ }
                        .onFailure {
                            // Сохраняем книгу даже без онлайн-обогащения: повторяем импорт в «бедном» режиме.
                            runCatching { importOne(d, enrich = false) }
                                .onSuccess { added++ }
                                .onFailure { e2 -> failures += "${d.title}: ${e2.message ?: e2::class.java.simpleName}" }
                        }
                } catch (e: Exception) {
                    failures += "${d.title}: ${e.message ?: e::class.java.simpleName}"
                }
            }
            return ScanResult(found = descriptors.size, added = added, skipped = skipped, failures = failures)
        }

        private suspend fun importOne(
            d: BookDescriptor,
            enrich: Boolean = true,
        ) {
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
            // В тегах обложки нет — берём файл-обложку из папки книги (cover.jpg и т.п.).
            if (cover == null && d.coverImage != null) {
                cover = readCoverBytes(d.coverImage.uri)?.let { coverStore.saveBytes(it, d.sourceUri) }
            }
            var genre = meta.genre
            var year = meta.year

            if (enrich && PlayerSettings.onlineEnrichment && (author.isNullOrBlank() || description.isNullOrBlank() || cover == null)) {
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
                    status = BookStatus.WISHLIST,
                    sourceKind = d.sourceKind,
                    originalPath = null,
                    manuallyEdited = false,
                )
            bookRepository.upsert(book, chapters)
        }

        private fun readCoverBytes(uri: String): ByteArray? =
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }
            }.getOrNull()

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

/** Итог сканирования: сколько книг распознано, сколько новых импортировано,
 *  сколько уже было в каталоге и какие книги не удалось импортировать (с причинами). */
data class ScanResult(
    val found: Int,
    val added: Int,
    val skipped: Int = 0,
    val failures: List<String> = emptyList(),
)

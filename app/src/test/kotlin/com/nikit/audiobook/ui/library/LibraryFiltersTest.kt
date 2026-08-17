package com.nikit.audiobook.ui.library

import com.google.common.truth.Truth.assertThat
import com.nikit.audiobook.domain.model.Book
import com.nikit.audiobook.domain.model.BookStatus
import com.nikit.audiobook.domain.model.FileType
import com.nikit.audiobook.domain.model.PlaybackProgress
import com.nikit.audiobook.domain.model.SourceKind
import org.junit.Test

class LibraryFiltersTest {
    private fun book(
        id: String,
        title: String,
        author: String? = null,
        status: BookStatus = BookStatus.READING,
        filesPresent: Boolean = true,
        addedAt: Long = 0L,
    ): Book =
        Book(
            id = id,
            title = title,
            sourceUri = "uri://$id",
            fileType = FileType.FOLDER,
            totalDurationMs = 100_000L,
            status = status,
            sourceKind = SourceKind.LOCAL_FOLDER,
            author = author,
            filesPresent = filesPresent,
            addedAt = addedAt,
        )

    private val books =
        listOf(
            book("1", "Дюна", "Фрэнк Герберт"),
            book("2", "Дюна: Мессия", "Фрэнк Герберт"),
            book("3", "Мастер и Маргарита", "Булгаков"),
            book("4", "Задача трёх тел", "Лю Цысинь", filesPresent = false),
        )

    @Test
    fun searchMatchesTitleAndAuthorCaseInsensitive() {
        val byTitle = applyLibraryFilters(books, emptyMap(), LibrarySort.TITLE, null, false, "дюн")
        assertThat(byTitle.map { it.id }).containsExactly("1", "2")

        val byAuthor = applyLibraryFilters(books, emptyMap(), LibrarySort.TITLE, null, false, "герб")
        assertThat(byAuthor.map { it.id }).containsExactly("1", "2")

        // пустой/пробельный запрос не фильтрует
        val all = applyLibraryFilters(books, emptyMap(), LibrarySort.TITLE, null, false, "   ")
        assertThat(all).hasSize(4)
    }

    @Test
    fun statusFilterAndOnlyPresentApply() {
        val onlyPresent = applyLibraryFilters(books, emptyMap(), LibrarySort.TITLE, null, true, "")
        assertThat(onlyPresent.map { it.id }).containsExactly("1", "2", "3")

        val withStatus = applyLibraryFilters(books, emptyMap(), LibrarySort.TITLE, BookStatus.READING, false, "")
        assertThat(withStatus.map { it.id }).containsExactly("1", "2", "3", "4")
    }

    @Test
    fun sortByTitleAuthorAndProgress() {
        val byTitle = applyLibraryFilters(books, emptyMap(), LibrarySort.TITLE, null, false, "")
        assertThat(byTitle.first().title).isEqualTo("Дюна")
        assertThat(byTitle.last().title).isEqualTo("Мастер и Маргарита")

        val byAuthor = applyLibraryFilters(books, emptyMap(), LibrarySort.AUTHOR, null, false, "")
        assertThat(byAuthor.first().author).isEqualTo("Булгаков")

        val progress =
            mapOf(
                "1" to PlaybackProgress(bookId = "1", chapterIndex = 0, positionMs = 0, percent = 0.1f),
                "2" to PlaybackProgress(bookId = "2", chapterIndex = 0, positionMs = 0, percent = 0.9f),
            )
        val byProgress = applyLibraryFilters(books, progress, LibrarySort.PROGRESS, null, false, "")
        assertThat(byProgress.first().id).isEqualTo("2")
        // книги без прогресса (percent 0) — в конце
        assertThat(byProgress.takeLast(2).map { it.id }).containsExactly("3", "4").inOrder()
    }
}

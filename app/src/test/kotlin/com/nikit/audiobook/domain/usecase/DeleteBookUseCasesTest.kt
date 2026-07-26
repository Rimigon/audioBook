package com.nikit.audiobook.domain.usecase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nikit.audiobook.data.db.AudioBookDatabase
import com.nikit.audiobook.data.repo.BookRepository
import com.nikit.audiobook.domain.model.Book
import com.nikit.audiobook.domain.model.BookStatus
import com.nikit.audiobook.domain.model.Chapter
import com.nikit.audiobook.domain.model.FileType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeleteBookUseCasesTest {
    private lateinit var db: AudioBookDatabase
    private lateinit var repo: BookRepository
    private lateinit var deleteFiles: DeleteBookFiles
    private lateinit var deleteFromCatalog: DeleteBookFromCatalog

    @Before fun setup() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    AudioBookDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        repo = BookRepository(db.bookDao(), db.chapterDao())
        deleteFiles = DeleteBookFiles(repo)
        deleteFromCatalog = DeleteBookFromCatalog(repo)
    }

    @After fun teardown() {
        db.close()
    }

    private suspend fun seed(): String {
        val book =
            Book(
                id = "b1",
                title = "Дюна",
                author = "Герберт",
                sourceUri = "uri://dune",
                fileType = FileType.FOLDER,
                status = BookStatus.READING,
            )
        repo.upsert(
            book,
            listOf(
                Chapter(bookId = "b1", index = 0, title = "Ch0", startMs = 0, endMs = 1000, filePath = "f0"),
            ),
        )
        return book.id
    }

    @Test fun deleteFiles_keepsCard() =
        runTest {
            val id = seed()
            deleteFiles(id)
            val card = repo.getBook(id)
            assertThat(card).isNotNull()
            assertThat(card!!.filesPresent).isFalse()
            assertThat(card.sourceUri).isNull()
        }

    @Test fun deleteFromCatalog_removesCard() =
        runTest {
            val id = seed()
            deleteFromCatalog(id)
            assertThat(repo.getBook(id)).isNull()
            assertThat(repo.observeAll().first()).isEmpty()
        }
}

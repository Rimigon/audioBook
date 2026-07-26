package com.nikit.audiobook.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nikit.audiobook.data.db.AudioBookDatabase
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
class BookRepositoryTest {
    private lateinit var db: AudioBookDatabase
    private lateinit var repo: BookRepository

    @Before fun setup() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    AudioBookDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        repo = BookRepository(db.bookDao(), db.chapterDao())
    }

    @After fun teardown() {
        db.close()
    }

    private fun sampleBook(id: String = "b1") =
        Book(
            id = id,
            title = "Дюна",
            author = "Герберт",
            sourceUri = "uri://dune",
            fileType = FileType.FOLDER,
            status = BookStatus.READING,
        )

    @Test fun upsertPersistsBookAndChapters() =
        runTest {
            repo.upsert(
                sampleBook(),
                listOf(
                    Chapter(bookId = "b1", index = 0, title = "Ch0", startMs = 0, endMs = 1000, filePath = "f0"),
                ),
            )
            val loaded = repo.getBook("b1")
            assertThat(loaded?.title).isEqualTo("Дюна")
        }

    @Test fun markFilesDeleted_keepsCard_clearsFiles() =
        runTest {
            repo.upsert(
                sampleBook(),
                listOf(
                    Chapter(bookId = "b1", index = 0, title = "Ch0", startMs = 0, endMs = 1000, filePath = "f0"),
                ),
            )
            repo.markFilesDeleted("b1")
            val card = repo.getBook("b1")
            assertThat(card).isNotNull()
            assertThat(card!!.filesPresent).isFalse()
            assertThat(card.sourceUri).isNull()
        }

    @Test fun deleteBookPermanently_removesEverything() =
        runTest {
            repo.upsert(
                sampleBook(),
                listOf(
                    Chapter(bookId = "b1", index = 0, title = "Ch0", startMs = 0, endMs = 1000, filePath = "f0"),
                ),
            )
            repo.deleteBookPermanently("b1")
            assertThat(repo.getBook("b1")).isNull()
            assertThat(repo.observeAll().first()).isEmpty()
        }
}

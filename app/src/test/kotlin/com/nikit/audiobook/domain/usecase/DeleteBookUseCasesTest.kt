package com.nikit.audiobook.domain.usecase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nikit.audiobook.data.db.AudioBookDatabase
import com.nikit.audiobook.data.repo.BookRepository
import com.nikit.audiobook.data.repo.ChapterRepository
import com.nikit.audiobook.data.saf.FileDeleter
import com.nikit.audiobook.domain.model.Book
import com.nikit.audiobook.domain.model.BookStatus
import com.nikit.audiobook.domain.model.Chapter
import com.nikit.audiobook.domain.model.FileType
import com.nikit.audiobook.domain.model.SourceKind
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
    private lateinit var chapterRepo: ChapterRepository
    private lateinit var deleteFiles: DeleteBookFiles
    private lateinit var deleteFromCatalog: DeleteBookFromCatalog

    private val deletedUris = mutableListOf<List<String>>()
    private var deleteResult = true
    private val fileDeleter =
        FileDeleter { uris ->
            deletedUris += uris
            deleteResult
        }

    @Before fun setup() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    AudioBookDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        repo = BookRepository(db.bookDao(), db.chapterDao())
        chapterRepo = ChapterRepository(db.chapterDao())
        deleteFiles = DeleteBookFiles(repo, chapterRepo, fileDeleter)
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
                sourceUri = "content://tree/root/document/dune",
                fileType = FileType.FOLDER,
                status = BookStatus.READING,
                sourceKind = SourceKind.LOCAL_FOLDER,
            )
        repo.upsert(
            book,
            listOf(
                Chapter(
                    bookId = "b1",
                    index = 0,
                    title = "Ch0",
                    startMs = 0,
                    endMs = 1000,
                    filePath = "content://tree/root/document/dune/ch0.mp3",
                ),
                Chapter(
                    bookId = "b1",
                    index = 1,
                    title = "Ch1",
                    startMs = 1000,
                    endMs = 2000,
                    filePath = "content://tree/root/document/dune/ch1.mp3",
                ),
            ),
        )
        return book.id
    }

    @Test fun deleteFiles_deletesRealFilesThenMarksCard() =
        runTest {
            val id = seed()
            val ok = deleteFiles(id)
            assertThat(ok).isTrue()
            // файлы (и папка книги) реально удаляются до пометки карточки
            assertThat(deletedUris).hasSize(1)
            assertThat(deletedUris[0])
                .containsExactly(
                    "content://tree/root/document/dune",
                    "content://tree/root/document/dune/ch0.mp3",
                    "content://tree/root/document/dune/ch1.mp3",
                ).inOrder()
            val card = repo.getBook(id)
            assertThat(card).isNotNull()
            assertThat(card!!.filesPresent).isFalse()
            assertThat(card.sourceUri).isNull()
        }

    @Test fun deleteFiles_failureLeavesFilesAndCardIntact() =
        runTest {
            val id = seed()
            deleteResult = false
            val ok = deleteFiles(id)
            assertThat(ok).isFalse()
            val card = repo.getBook(id)
            assertThat(card!!.filesPresent).isTrue()
            assertThat(card.sourceUri).isEqualTo("content://tree/root/document/dune")
        }

    @Test fun deleteFromCatalog_removesCard() =
        runTest {
            val id = seed()
            deleteFromCatalog(id)
            assertThat(repo.getBook(id)).isNull()
            assertThat(repo.observeAll().first()).isEmpty()
        }
}

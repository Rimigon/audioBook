package com.nikit.audiobook.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nikit.audiobook.data.db.dao.BookDao
import com.nikit.audiobook.data.db.entity.BookEntity
import com.nikit.audiobook.domain.model.BookStatus
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
class BookDaoTest {
    private lateinit var db: AudioBookDatabase
    private lateinit var dao: BookDao

    @Before fun setup() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    AudioBookDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        dao = db.bookDao()
    }

    @After fun teardown() {
        db.close()
    }

    private fun book(
        id: String,
        title: String = "T",
        filesPresent: Boolean = true,
    ) = BookEntity(
        id = id,
        title = title,
        author = null,
        series = null,
        seriesIndex = null,
        genre = null,
        description = null,
        year = null,
        coverPath = null,
        sourceUri = "uri://$id",
        filesPresent = filesPresent,
        fileType = FileType.SINGLE_FILE,
        totalDurationMs = 0L,
        addedAt = 1L,
        lastPlayedAt = null,
        completedAt = null,
        rating = 0,
        review = null,
        status = BookStatus.WISHLIST,
        sourceKind = SourceKind.LOCAL_FILE,
        originalPath = null,
        manuallyEdited = false,
    )

    @Test fun upsertAndObserve() =
        runTest {
            dao.upsert(book("a"))
            assertThat(dao.observeAll().first()).hasSize(1)
            assertThat(dao.getById("a")?.title).isEqualTo("T")
        }

    @Test fun markFilesDeleted_keepsRow_clearsSourceUri() =
        runTest {
            dao.upsert(book("a"))
            dao.markFilesDeleted("a")
            val b = dao.getById("a")
            assertThat(b).isNotNull()
            assertThat(b!!.filesPresent).isFalse()
            assertThat(b.sourceUri).isNull()
        }

    @Test fun deleteById_removesRow() =
        runTest {
            dao.upsert(book("a"))
            dao.deleteById("a")
            assertThat(dao.observeAll().first()).isEmpty()
        }

    @Test fun markPlayed_setsStatusReadingAndKeepsCompleted() =
        runTest {
            dao.upsert(book("a"))
            dao.markPlayed("a", 123L)
            assertThat(dao.getById("a")!!.status).isEqualTo(BookStatus.READING)
            assertThat(dao.getById("a")!!.lastPlayedAt).isEqualTo(123L)
            dao.markCompleted("a", 200L)
            dao.markPlayed("a", 300L)
            // повторная отметка не сбрасывает COMPLETED
            assertThat(dao.getById("a")!!.status).isEqualTo(BookStatus.COMPLETED)
        }
}

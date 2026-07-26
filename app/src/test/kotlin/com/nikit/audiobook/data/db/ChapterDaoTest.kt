package com.nikit.audiobook.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nikit.audiobook.data.db.dao.BookDao
import com.nikit.audiobook.data.db.dao.ChapterDao
import com.nikit.audiobook.data.db.entity.BookEntity
import com.nikit.audiobook.data.db.entity.ChapterEntity
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
class ChapterDaoTest {
    private lateinit var db: AudioBookDatabase
    private lateinit var bookDao: BookDao
    private lateinit var chapterDao: ChapterDao

    @Before fun setup() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    AudioBookDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        bookDao = db.bookDao()
        chapterDao = db.chapterDao()
    }

    @After fun teardown() {
        db.close()
    }

    private fun book(id: String) =
        BookEntity(
            id,
            "T",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "uri://$id",
            true,
            FileType.SINGLE_FILE,
            0L,
            1L,
            null,
            null,
            0,
            null,
            BookStatus.WISHLIST,
            SourceKind.LOCAL_FILE,
            null,
            false,
        )

    @Test fun insertAndObserve() =
        runTest {
            bookDao.upsert(book("a"))
            chapterDao.insertAll(
                listOf(
                    ChapterEntity("c1", "a", 0, "Ch0", 0L, 1000L, "file://c1"),
                    ChapterEntity("c2", "a", 1, "Ch1", 1000L, 2000L, "file://c2"),
                ),
            )
            assertThat(chapterDao.observeByBook("a").first()).hasSize(2)
        }

    @Test fun clearFilePathsForBook_nullsPaths_keepsRows() =
        runTest {
            bookDao.upsert(book("a"))
            chapterDao.insertAll(listOf(ChapterEntity("c1", "a", 0, "Ch0", 0L, 1000L, "file://c1")))
            chapterDao.clearFilePathsForBook("a")
            val rows = chapterDao.getByBook("a")
            assertThat(rows).hasSize(1)
            assertThat(rows.first().filePath).isNull()
        }

    @Test fun deletingBook_cascadesChapters() =
        runTest {
            bookDao.upsert(book("a"))
            chapterDao.insertAll(listOf(ChapterEntity("c1", "a", 0, "Ch0", 0L, 1000L, "file://c1")))
            bookDao.deleteById("a")
            assertThat(chapterDao.getByBook("a")).isEmpty()
        }
}

package com.nikit.audiobook.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nikit.audiobook.data.db.dao.BookDao
import com.nikit.audiobook.data.db.dao.BookmarkDao
import com.nikit.audiobook.data.db.entity.BookEntity
import com.nikit.audiobook.data.db.entity.BookmarkEntity
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
class BookmarkDaoTest {
    private lateinit var db: AudioBookDatabase
    private lateinit var bookDao: BookDao
    private lateinit var bookmarkDao: BookmarkDao

    @Before fun setup() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    AudioBookDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        bookDao = db.bookDao()
        bookmarkDao = db.bookmarkDao()
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

    @Test fun upsertAndObserve() =
        runTest {
            bookDao.upsert(book("a"))
            bookmarkDao.upsert(BookmarkEntity("bm1", "a", 5000L, "Отметка", "заметка", 1L))
            assertThat(bookmarkDao.observeByBook("a").first()).hasSize(1)
        }

    @Test fun deletingBook_cascadesBookmarks() =
        runTest {
            bookDao.upsert(book("a"))
            bookmarkDao.upsert(BookmarkEntity("bm1", "a", 5000L, "x", null, 1L))
            bookDao.deleteById("a")
            assertThat(bookmarkDao.observeByBook("a").first()).isEmpty()
        }
}

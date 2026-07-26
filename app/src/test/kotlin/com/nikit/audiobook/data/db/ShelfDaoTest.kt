package com.nikit.audiobook.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nikit.audiobook.data.db.dao.BookDao
import com.nikit.audiobook.data.db.dao.ShelfDao
import com.nikit.audiobook.data.db.entity.BookEntity
import com.nikit.audiobook.data.db.entity.ShelfEntity
import com.nikit.audiobook.data.db.entity.ShelfMembershipEntity
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
class ShelfDaoTest {
    private lateinit var db: AudioBookDatabase
    private lateinit var bookDao: BookDao
    private lateinit var shelfDao: ShelfDao

    @Before fun setup() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    AudioBookDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        bookDao = db.bookDao()
        shelfDao = db.shelfDao()
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

    @Test fun shelfAndMembershipObserve() =
        runTest {
            bookDao.upsert(book("a"))
            shelfDao.upsert(ShelfEntity("s1", "Прочитал", "#aa0000", 0))
            shelfDao.addMembership(ShelfMembershipEntity("s1", "a"))
            assertThat(shelfDao.observeAll().first()).hasSize(1)
            assertThat(shelfDao.observeBooksOfShelf("s1").first()).hasSize(1)
        }

    @Test fun deletingBook_cascadesMembership() =
        runTest {
            bookDao.upsert(book("a"))
            shelfDao.upsert(ShelfEntity("s1", "Прочитал", null, 0))
            shelfDao.addMembership(ShelfMembershipEntity("s1", "a"))
            bookDao.deleteById("a")
            assertThat(shelfDao.observeBooksOfShelf("s1").first()).isEmpty()
        }
}

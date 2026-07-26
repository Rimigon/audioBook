package com.nikit.audiobook.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nikit.audiobook.data.db.dao.BookDao
import com.nikit.audiobook.data.db.dao.TagDao
import com.nikit.audiobook.data.db.entity.BookEntity
import com.nikit.audiobook.data.db.entity.BookTagEntity
import com.nikit.audiobook.data.db.entity.TagEntity
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
class TagDaoTest {
    private lateinit var db: AudioBookDatabase
    private lateinit var bookDao: BookDao
    private lateinit var tagDao: TagDao

    @Before fun setup() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    AudioBookDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        bookDao = db.bookDao()
        tagDao = db.tagDao()
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

    @Test fun tagAndLinkObserve() =
        runTest {
            bookDao.upsert(book("a"))
            tagDao.upsert(TagEntity("t1", "фэнтези"))
            tagDao.link(BookTagEntity("t1", "a"))
            assertThat(tagDao.observeTagsOfBook("a").first()).hasSize(1)
        }

    @Test fun deletingBook_cascadesTagLinks() =
        runTest {
            bookDao.upsert(book("a"))
            tagDao.upsert(TagEntity("t1", "фэнтези"))
            tagDao.link(BookTagEntity("t1", "a"))
            bookDao.deleteById("a")
            assertThat(tagDao.observeTagsOfBook("a").first()).isEmpty()
            // тег остаётся
            assertThat(tagDao.observeAll().first()).hasSize(1)
        }
}

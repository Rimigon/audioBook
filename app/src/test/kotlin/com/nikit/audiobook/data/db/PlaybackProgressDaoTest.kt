package com.nikit.audiobook.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nikit.audiobook.data.db.dao.BookDao
import com.nikit.audiobook.data.db.dao.PlaybackProgressDao
import com.nikit.audiobook.data.db.entity.BookEntity
import com.nikit.audiobook.data.db.entity.PlaybackProgressEntity
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
class PlaybackProgressDaoTest {
    private lateinit var db: AudioBookDatabase
    private lateinit var bookDao: BookDao
    private lateinit var progressDao: PlaybackProgressDao

    @Before fun setup() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    AudioBookDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        bookDao = db.bookDao()
        progressDao = db.playbackProgressDao()
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

    @Test fun upsertReplacesByBookId() =
        runTest {
            bookDao.upsert(book("a"))
            progressDao.upsert(PlaybackProgressEntity("a", 1000L, 0, 0.1f, 1L))
            progressDao.upsert(PlaybackProgressEntity("a", 2000L, 1, 0.2f, 2L))
            val p = progressDao.getByBook("a")
            assertThat(p?.positionMs).isEqualTo(2000L)
        }

    @Test fun deletingBook_cascadesProgress() =
        runTest {
            bookDao.upsert(book("a"))
            progressDao.upsert(PlaybackProgressEntity("a", 1000L, 0, 0.1f, 1L))
            bookDao.deleteById("a")
            assertThat(progressDao.getByBook("a")).isNull()
        }

    @Test fun observeEmitsChanges() =
        runTest {
            bookDao.upsert(book("a"))
            progressDao.upsert(PlaybackProgressEntity("a", 1000L, 0, 0.1f, 1L))
            assertThat(progressDao.observeByBook("a").first()?.positionMs).isEqualTo(1000L)
        }
}

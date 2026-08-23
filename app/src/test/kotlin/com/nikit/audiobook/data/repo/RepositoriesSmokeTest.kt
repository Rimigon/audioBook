package com.nikit.audiobook.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nikit.audiobook.data.db.AudioBookDatabase
import com.nikit.audiobook.data.db.entity.BookEntity
import com.nikit.audiobook.domain.model.BookStatus
import com.nikit.audiobook.domain.model.FileType
import com.nikit.audiobook.domain.model.PlaybackProgress
import com.nikit.audiobook.domain.model.SourceKind
import com.nikit.audiobook.domain.model.Tag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RepositoriesSmokeTest {
    private lateinit var db: AudioBookDatabase
    private lateinit var progress: ProgressRepository
    private lateinit var tags: TagRepository
    private lateinit var books: BookRepository

    @Before fun setup() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    AudioBookDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        progress = ProgressRepository(db.playbackProgressDao())
        tags = TagRepository(db.tagDao())
        books = BookRepository(db.bookDao(), db.chapterDao())
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

    @Test fun progressFlow() =
        runTest {
            db.bookDao().upsert(book("a"))
            progress.upsert(PlaybackProgress("a", 1000L, 0, 0.1f, 1L))
            assertThat(progress.get("a")?.positionMs).isEqualTo(1000L)
        }

    @Test fun tagFlow() =
        runTest {
            db.bookDao().upsert(book("a"))
            tags.upsert(Tag("t1", "фэнтези"))
            tags.link("t1", "a")
            assertThat(tags.observeTagsOfBook("a").first()).hasSize(1)
        }
}

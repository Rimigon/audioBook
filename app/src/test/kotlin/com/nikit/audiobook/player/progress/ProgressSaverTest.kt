package com.nikit.audiobook.player.progress

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nikit.audiobook.data.db.AudioBookDatabase
import com.nikit.audiobook.data.repo.BookRepository
import com.nikit.audiobook.data.repo.ProgressRepository
import com.nikit.audiobook.domain.model.Book
import com.nikit.audiobook.domain.model.BookStatus
import com.nikit.audiobook.domain.model.FileType
import com.nikit.audiobook.domain.model.SourceKind
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProgressSaverTest {
    private lateinit var db: AudioBookDatabase
    private lateinit var books: BookRepository
    private lateinit var progress: ProgressRepository
    private lateinit var coord: ProgressSaverCoordinator

    @Before fun setup() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    AudioBookDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        books = BookRepository(db.bookDao(), db.chapterDao())
        progress = ProgressRepository(db.playbackProgressDao())
        coord = ProgressSaverCoordinator(progress, ProgressSaver(intervalMs = 10_000), intervalMs = 1_000)
    }

    @After fun teardown() {
        db.close()
    }

    private suspend fun seed(): String {
        val b =
            Book(
                id = "b1",
                title = "T",
                sourceUri = "uri://b1",
                fileType = FileType.SINGLE_FILE,
                status = BookStatus.READING,
                sourceKind = SourceKind.LOCAL_FILE,
            )
        books.upsert(b)
        return b.id
    }

    @Test fun throttle_doesNotSaveWithinInterval() =
        runTest {
            val id = seed()
            coord.tick(id, 1000L, 0, 100_000L, nowMs = 0L)
            coord.tick(id, 2000L, 0, 100_000L, nowMs = 5_000L) // меньше 10с → не пишем
            assertThat(progress.get(id)?.positionMs).isEqualTo(1000L)
        }

    @Test fun throttle_savesAfterInterval() =
        runTest {
            val id = seed()
            coord.tick(id, 1000L, 0, 100_000L, nowMs = 0L)
            coord.tick(id, 5000L, 0, 100_000L, nowMs = 11_000L) // >=10с → пишем
            assertThat(progress.get(id)?.positionMs).isEqualTo(5000L)
        }

    @Test fun stopForcesSave() =
        runTest {
            val id = seed()
            coord.tick(id, 1000L, 0, 100_000L, nowMs = 0L)
            coord.tick(id, 2000L, 0, 100_000L, nowMs = 1_000L)
            coord.stop(2500L, 0, 100_000L)
            assertThat(progress.get(id)?.positionMs).isEqualTo(2500L)
        }

    @Test fun savesPercentAndChapter() =
        runTest {
            val id = seed()
            coord.tick(id, 50_000L, 2, 100_000L, nowMs = 0L)
            assertThat(progress.get(id)?.percent).isWithin(0.001f).of(0.5f)
            assertThat(progress.get(id)?.chapterIndex).isEqualTo(2)
        }
}

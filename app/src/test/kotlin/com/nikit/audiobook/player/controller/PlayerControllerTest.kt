package com.nikit.audiobook.player.controller

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nikit.audiobook.data.db.AudioBookDatabase
import com.nikit.audiobook.data.repo.BookRepository
import com.nikit.audiobook.data.repo.BookmarkRepository
import com.nikit.audiobook.data.repo.ChapterRepository
import com.nikit.audiobook.data.repo.ProgressRepository
import com.nikit.audiobook.data.saf.ScanSettings
import com.nikit.audiobook.domain.model.Book
import com.nikit.audiobook.domain.model.BookStatus
import com.nikit.audiobook.domain.model.Chapter
import com.nikit.audiobook.domain.model.FileType
import com.nikit.audiobook.domain.model.SourceKind
import com.nikit.audiobook.player.effects.EqualizerPreset
import com.nikit.audiobook.player.effects.VolumeBoost
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerControllerTest {
    private lateinit var db: AudioBookDatabase
    private lateinit var books: BookRepository
    private lateinit var chapters: ChapterRepository
    private lateinit var progress: ProgressRepository
    private lateinit var bookmarks: BookmarkRepository
    private lateinit var settings: ScanSettings
    private lateinit var controller: PlayerController
    private val engine = FakeEngine()

    @Before fun setup() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    AudioBookDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        books = BookRepository(db.bookDao(), db.chapterDao())
        chapters = ChapterRepository(db.chapterDao())
        progress = ProgressRepository(db.playbackProgressDao())
        bookmarks = BookmarkRepository(db.bookmarkDao())
        settings = ScanSettings(ApplicationProvider.getApplicationContext())
        controller = PlayerController(books, chapters, progress, bookmarks, settings)
        controller.attach(engine)
    }

    @After fun teardown() {
        controller.detach()
        db.close()
        VolumeBoost.gain = 1f
        PlayerSettings.defaultVolumeBoost = 1f
        PlayerSettings.defaultSpeed = 1f
        PlayerSettings.autoResume = true
        PlayerSettings.currentEqualizerPreset = null
        PlayerSettings.audioSessionId = 0
    }

    private suspend fun seed(): String {
        val b =
            Book(
                id = "b1",
                title = "Дюна",
                sourceUri = "uri://dune",
                fileType = FileType.FOLDER,
                totalDurationMs = 100_000L,
                status = BookStatus.READING,
                sourceKind = SourceKind.LOCAL_FOLDER,
            )
        books.upsert(
            b,
            listOf(
                Chapter(bookId = "b1", index = 0, title = "Ch0", startMs = 0, endMs = 50000, filePath = "uri://0"),
                Chapter(bookId = "b1", index = 1, title = "Ch1", startMs = 50000, endMs = 100000, filePath = "uri://1"),
            ),
        )
        return b.id
    }

    @Test fun loadBookSetsMediaItemsAndPlays() =
        runTest {
            val id = seed()
            controller.loadBook(id)
            assertThat(engine._items).containsExactly("uri://0", "uri://1").inOrder()
            assertThat(engine._playing).isTrue()
            assertThat(controller.state.value.title).isEqualTo("Дюна")
            assertThat(controller.state.value.isPlaying).isTrue()
        }

    @Test fun tickSavesProgress() =
        runTest {
            val id = seed()
            controller.loadBook(id)
            engine._pos = 12_000L
            engine._chapter = 1
            controller.tick(nowMs = 1000L)
            controller.tick(nowMs = 12_000L) // >= 10с → второе сохранение
            val p = progress.get(id)
            assertThat(p).isNotNull()
            assertThat(p!!.chapterIndex).isEqualTo(1)
        }

    @Test fun pauseSavesAndUpdatesState() =
        runTest {
            val id = seed()
            controller.loadBook(id)
            engine._pos = 5_000L
            controller.pause()
            assertThat(engine._playing).isFalse()
            assertThat(controller.state.value.isPlaying).isFalse()
            assertThat(progress.get(id)?.positionMs).isEqualTo(5_000L)
        }

    @Test fun setSpeedClampsAndUpdatesState() =
        runTest {
            val id = seed()
            controller.loadBook(id)
            controller.setSpeed(5f)
            assertThat(engine._speed).isEqualTo(4.0f)
            assertThat(controller.state.value.speed).isEqualTo(4.0f)
        }

    @Test fun sleepTimerPausesOnExpiry() =
        runTest {
            seed()
            controller.loadBook("b1")
            assertThat(engine._playing).isTrue()
            controller.startSleep(0L) // истекает сразу
            controller.tick(nowMs = 1L)
            assertThat(engine._playing).isFalse()
        }

    @Test fun loadBookMarksPlayed() =
        runTest {
            val id = seed()
            controller.loadBook(id)
            val b = books.getBook(id)
            assertThat(b!!.lastPlayedAt).isNotNull()
            assertThat(b.status).isEqualTo(BookStatus.READING)
        }

    @Test fun tickAtEndMarksCompleted() =
        runTest {
            val id = seed()
            controller.loadBook(id)
            engine._pos = engine._dur
            engine._playing = false
            controller.tick(nowMs = 2_000L)
            val b = books.getBook(id)
            assertThat(b!!.status).isEqualTo(BookStatus.COMPLETED)
            assertThat(b.completedAt).isNotNull()
        }

    @Test fun completedBookKeepsCompletedOnReListen() =
        runTest {
            val id = seed()
            controller.loadBook(id)
            engine._pos = engine._dur
            engine._playing = false
            controller.tick(nowMs = 2_000L)
            // дослушанную книгу запускают заново — статус COMPLETED не сбрасывается
            engine._pos = 0L
            engine._playing = true
            controller.loadBook(id)
            val b = books.getBook(id)
            assertThat(b!!.status).isEqualTo(BookStatus.COMPLETED)
            assertThat(b.lastPlayedAt).isNotNull()
        }

    @Test fun addBookmarkPersists() =
        runTest {
            val id = seed()
            controller.loadBook(id)
            engine._pos = 33_000L
            val bm = controller.addBookmark("Отметка", "заметка")
            assertThat(bm).isNotNull()
            assertThat(bm!!.positionMs).isEqualTo(33_000L)
            assertThat(bookmarks.observeManualByBook(id).first()).hasSize(1)
        }

    @Test
    fun dataStoreWriteReadSanity() =
        runTest {
            settings.setVolumeBoost(1.5f) // напрямую, без main-лоопера
            val saved = settings.volumeBoost.first { it == 1.5f }
            assertThat(saved).isEqualTo(1.5f)
        }

    @Test
    fun volumeBoostPersistedAndAppliedOnLoad() =
        runTest {
            val id = seed()
            // Сохраняем усиление через контроллер (запись идёт на settingsScope/IO).
            controller.setVolumeBoost(1.5f)
            // Ждём, пока запись реально доедет до DataStore.
            val saved = settings.volumeBoost.first { it == 1.5f }
            assertThat(saved).isEqualTo(1.5f)

            // «Перезапуск»: отключаем контроллер и сбрасываем статики — восстановление
            // теперь выполняет loadBook (детерминированное чтение из DataStore).
            controller.detach()
            VolumeBoost.gain = 1f
            PlayerSettings.defaultVolumeBoost = 1f

            val restored = PlayerController(books, chapters, progress, bookmarks, settings)
            restored.attach(engine)
            restored.loadBook(id)

            // Сохранённое усиление применено и к звуку, и к состоянию UI.
            assertThat(engine._volume).isEqualTo(1.0f) // volume ≤ 1 (ограничение Media3)
            assertThat(VolumeBoost.gain).isEqualTo(1.5f)
            assertThat(restored.state.value.volumeBoost).isEqualTo(1.5f)
            restored.detach()
        }

    @Test
    fun speedPersistedAndAppliedOnLoad() =
        runTest {
            val id = seed()
            controller.setSpeed(1.5f)
            val saved = settings.playbackSpeed.first { it == 1.5f }
            assertThat(saved).isEqualTo(1.5f)

            controller.detach()
            PlayerSettings.defaultSpeed = 1f

            val restored = PlayerController(books, chapters, progress, bookmarks, settings)
            restored.attach(engine)
            restored.loadBook(id)

            assertThat(engine._speed).isEqualTo(1.5f)
            assertThat(restored.state.value.speed).isEqualTo(1.5f)
            restored.detach()
        }

    @Test
    fun loadBookRespectsAutoResumeSetting() =
        runTest {
            val id = seed()
            PlayerSettings.autoResume = false
            controller.loadBook(id)
            assertThat(engine._playing).isFalse()
            assertThat(controller.state.value.isPlaying).isFalse()

            PlayerSettings.autoResume = true
            controller.loadBook(id)
            assertThat(engine._playing).isTrue()
            assertThat(controller.state.value.isPlaying).isTrue()
        }

    @Test
    fun setEqualizerUpdatesStateEvenWithoutAudioSession() =
        runTest {
            val id = seed()
            controller.loadBook(id)
            controller.setEqualizer(EqualizerPreset.BASS_BOOST)
            assertThat(controller.state.value.equalizerPreset).isEqualTo(EqualizerPreset.BASS_BOOST)
            assertThat(PlayerSettings.currentEqualizerPreset).isEqualTo(EqualizerPreset.BASS_BOOST)
            // FLAT → пресет сброшен (null в настройках)
            controller.setEqualizer(EqualizerPreset.FLAT)
            assertThat(controller.state.value.equalizerPreset).isEqualTo(EqualizerPreset.FLAT)
        }
}

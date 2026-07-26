package com.nikit.audiobook.player.controller

import com.nikit.audiobook.data.repo.BookRepository
import com.nikit.audiobook.data.repo.BookmarkRepository
import com.nikit.audiobook.data.repo.ChapterRepository
import com.nikit.audiobook.data.repo.ProgressRepository
import com.nikit.audiobook.domain.model.Bookmark
import com.nikit.audiobook.player.effects.EqualizerPreset
import com.nikit.audiobook.player.effects.PlayerEffects
import com.nikit.audiobook.player.progress.ProgressSaverCoordinator
import com.nikit.audiobook.player.sleep.SleepDecision
import com.nikit.audiobook.player.sleep.SleepTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Настройки плеера (глобальные по умолчанию). В Подплане 4 хранятся в DataStore. */
object PlayerSettings {
    var defaultSpeed: Float = 1f
    var defaultVolumeBoost: Float = 1f
}

/**
 * Мозг плеера (UI-процесс). Загружает книгу в [PlayerEngine], стримит [PlayerUiState],
 * сохраняет прогресс (throttle ~10с + при паузе), управляет скоростью/громкостью,
 * таймером сна и закладками. UI подписывается на [state].
 *
 * [tick] — одна итерация опроса (вызывается сервисом/циклом каждые ~1с). Вынесен наружу
 * для управляемого тестирования.
 */
@Singleton
class PlayerController
    @Inject
    constructor(
        private val bookRepository: BookRepository,
        private val chapterRepository: ChapterRepository,
        private val progressRepository: ProgressRepository,
        private val bookmarkRepository: BookmarkRepository,
    ) {
        private var engine: PlayerEngine? = null
        private val _state = MutableStateFlow(PlayerUiState())
        val state: StateFlow<PlayerUiState> = _state.asStateFlow()

        private val saver = ProgressSaverCoordinator(progressRepository)
        private val sleep = SleepTimer()
        private var currentBookId: String? = null
        private var currentDurationMs: Long = 0L
        private var scope: CoroutineScope? = null
        private var pollJob: Job? = null

        fun attach(engine: PlayerEngine) {
            this.engine = engine
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        }

        fun detach() {
            pollJob?.cancel()
            scope?.cancel()
            scope = null
            engine = null
        }

        /** Начать воспроизведение книги (с авто-резьюмом). */
        suspend fun loadBook(bookId: String) {
            val e = engine ?: return
            val book = bookRepository.getBook(bookId) ?: return
            val chapters = chapterRepository.getByBook(bookId)
            if (chapters.isEmpty()) return
            currentBookId = bookId
            currentDurationMs = book.totalDurationMs
            val uris = chapters.mapNotNull { it.filePath }
            e.setMediaItems(uris)
            val progress = progressRepository.get(bookId)
            val startChapter = progress?.chapterIndex?.coerceIn(0, uris.size - 1) ?: 0
            e.seekTo(startChapter, progress?.positionMs ?: 0L)
            PlayerEffects.applySpeed(e, PlayerSettings.defaultSpeed)
            PlayerEffects.applyVolumeBoost(e, PlayerSettings.defaultVolumeBoost)
            _state.value =
                PlayerStateReducer.reduce(
                    _state.value,
                    PlayerEvent.BookLoaded(bookId, book.title, book.totalDurationMs, startChapter, book.filesPresent),
                )
            e.play()
            _state.value =
                PlayerStateReducer.reduce(_state.value, PlayerEvent.IsPlayingChanged(true))
            startPolling()
        }

        suspend fun pause() {
            val e = engine ?: return
            val bookId = currentBookId ?: return
            e.pause()
            saver.stop(bookId, e.positionMs, e.chapterIndex, currentDurationMs)
            _state.value = PlayerStateReducer.reduce(_state.value, PlayerEvent.IsPlayingChanged(false))
        }

        fun resume() {
            engine?.play()
        }

        fun seekTo(positionMs: Long) {
            engine?.seekTo(engine!!.chapterIndex, positionMs)
        }

        fun setSpeed(speed: Float) {
            val e = engine ?: return
            val clamped = speed.coerceIn(0.5f, 4.0f)
            PlayerEffects.applySpeed(e, clamped)
            _state.value = PlayerStateReducer.reduce(_state.value, PlayerEvent.SpeedChanged(clamped))
        }

        fun setVolumeBoost(boost: Float) {
            val e = engine ?: return
            val clamped = boost.coerceIn(1.0f, 2.0f)
            PlayerEffects.applyVolumeBoost(e, clamped)
            _state.value = PlayerStateReducer.reduce(_state.value, PlayerEvent.VolumeBoostChanged(clamped))
        }

        fun setEqualizer(preset: EqualizerPreset) {
            val sessionId = (engine as? MediaControllerEngine)?.audioSessionId() ?: return
            PlayerEffects.applyEqualizer(sessionId, preset)
        }

        fun startSleep(durationMs: Long) {
            sleep.startDuration(durationMs)
        }

        fun startSleepUntilChapterEnd(remainingChapterMs: Long) {
            sleep.startUntilChapterEnd(remainingChapterMs)
        }

        fun cancelSleep() {
            sleep.cancel()
            _state.value = PlayerStateReducer.reduce(_state.value, PlayerEvent.SleepLeftChanged(null))
        }

        suspend fun addBookmark(
            title: String,
            note: String? = null,
        ): Bookmark? {
            val e = engine ?: return null
            val bookId = currentBookId ?: return null
            val bm =
                Bookmark(
                    id = UUID.randomUUID().toString(),
                    bookId = bookId,
                    positionMs = e.positionMs,
                    title = title,
                    note = note,
                )
            bookmarkRepository.add(bm)
            return bm
        }

        suspend fun deleteBookmark(id: String) = bookmarkRepository.delete(id)

        /** Одна итерация опроса: обновляет состояние, прогресс, таймер сна. */
        suspend fun tick(nowMs: Long = System.currentTimeMillis()) {
            val e = engine ?: return
            _state.value = PlayerStateReducer.reduce(_state.value, PlayerEvent.IsPlayingChanged(e.isPlaying))
            _state.value = PlayerStateReducer.reduce(_state.value, PlayerEvent.PositionChanged(e.positionMs))
            _state.value = PlayerStateReducer.reduce(_state.value, PlayerEvent.ChapterChanged(e.chapterIndex))
            val bookId = currentBookId ?: return
            val dur = if (e.durationMs > 0) e.durationMs else currentDurationMs
            saver.tick(bookId, e.positionMs, e.chapterIndex, dur, nowMs)
            if (sleep.isRunning) {
                val decision = sleep.tick()
                when (decision) {
                    SleepDecision.Pause -> {
                        pause()
                    }

                    is SleepDecision.Continue -> {
                        _state.value =
                            PlayerStateReducer.reduce(
                                _state.value,
                                PlayerEvent.SleepLeftChanged((decision as SleepDecision.Continue).msLeft),
                            )
                    }
                }
            }
        }

        private fun startPolling() {
            pollJob?.cancel()
            pollJob =
                scope?.launch {
                    while (true) {
                        tick()
                        kotlinx.coroutines.delay(1_000L)
                    }
                }
        }
    }

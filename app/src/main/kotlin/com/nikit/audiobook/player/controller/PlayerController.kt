package com.nikit.audiobook.player.controller

import android.util.Log
import com.nikit.audiobook.data.repo.BookRepository
import com.nikit.audiobook.data.repo.BookmarkRepository
import com.nikit.audiobook.data.repo.ChapterRepository
import com.nikit.audiobook.data.repo.ProgressRepository
import com.nikit.audiobook.data.saf.ScanSettings
import com.nikit.audiobook.domain.model.Bookmark
import com.nikit.audiobook.domain.model.BookmarkKind
import com.nikit.audiobook.domain.model.Chapter
import com.nikit.audiobook.player.effects.EqualizerController
import com.nikit.audiobook.player.effects.EqualizerPreset
import com.nikit.audiobook.player.effects.PlayerEffects
import com.nikit.audiobook.player.effects.VolumeBoost
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Настройки плеера (глобальные по умолчанию). Усиление/скорость/эквалайзер хранятся в DataStore
 * (см. [ScanSettings] и детерминированное чтение в [PlayerController.loadBook]).
 * [audioSessionId]/[currentEqualizerPreset] — мост от сервиса к UI (один процесс). */
object PlayerSettings {
    var defaultSpeed: Float = 1f
    var defaultVolumeBoost: Float = 1f
    var seekStepMs: Long = 30_000L
    var autoResume: Boolean = true
    var onlineEnrichment: Boolean = true

    /** Audio session id ExoPlayer из сервиса (валиден, когда аудио реально играет). */
    var audioSessionId: Int = 0

    /** Текущий пресет эквалайзера — сервис применяет его при готовности аудио. */
    var currentEqualizerPreset: EqualizerPreset? = null
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
        private val scanSettings: ScanSettings,
    ) {
        private var engine: PlayerEngine? = null
        private val _state = MutableStateFlow(PlayerUiState())
        val state: StateFlow<PlayerUiState> = _state.asStateFlow()

        private val saver = ProgressSaverCoordinator(progressRepository)
        private val sleep = SleepTimer()
        private var currentBookId: String? = null
        private var currentDurationMs: Long = 0L
        private var completedNotified = false

        // Глобальные смещения начала каждой главы (накопленным итогом длительностей).
        // Нужны, чтобы таймлайн плеера был сквозным по всей книге, а не внутри одной главы.
        private var chapterOffsets: LongArray = LongArray(0)
        private var scope: CoroutineScope? = null
        private var pollJob: Job? = null

        // Свой скоуп для записи настроек в DataStore (IO), чтобы не блокировать main-поток.
        private val settingsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        // Точка прослушивания создаётся на паузе; если пользователь возобновил
        // воспроизведение быстро (в пределах SESSION_MERGE_MS) — точка удаляется
        // (короткая пауза не считается отдельной сессией).
        private var pendingSessionId: String? = null
        private var lastPauseAt: Long = 0L

        private companion object {
            const val SESSION_MERGE_MS = 120_000L
            val sessionDateFormat = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault())
        }

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
        suspend fun loadBook(
            bookId: String,
            startChapter: Int? = null,
            startPositionMs: Long? = null,
        ) {
            val e = engine ?: return
            val book = bookRepository.getBook(bookId) ?: return
            val chapters = chapterRepository.getByBook(bookId)
            if (chapters.isEmpty()) return
            currentBookId = bookId
            currentDurationMs = book.totalDurationMs
            completedNotified = false
            chapterOffsets = buildOffsets(chapters)
            val uris = chapters.mapNotNull { it.filePath }
            e.setMediaItems(uris)
            val progress = progressRepository.get(bookId)
            val chapter =
                if (startChapter != null) {
                    startChapter.coerceIn(0, (uris.size - 1).coerceAtLeast(0))
                } else {
                    progress?.chapterIndex?.coerceIn(0, (uris.size - 1).coerceAtLeast(0)) ?: 0
                }
            val position = startPositionMs ?: (if (startChapter != null) 0L else progress?.positionMs ?: 0L)
            e.seekTo(chapter, position)
            // Сохранённые настройки плеера читаем из DataStore детерминированно (с ожиданием),
            // чтобы после перезапуска не успеть применить дефолты до восстановления.
            val savedSpeed =
                runCatching { scanSettings.playbackSpeed.first() }
                    .getOrDefault(PlayerSettings.defaultSpeed)
            PlayerEffects.applySpeed(e, savedSpeed)
            PlayerSettings.defaultSpeed = savedSpeed
            _state.value =
                PlayerStateReducer.reduce(
                    _state.value,
                    PlayerEvent.SpeedChanged(savedSpeed),
                )
            val savedBoost =
                runCatching { scanSettings.volumeBoost.first() }
                    .getOrDefault(PlayerSettings.defaultVolumeBoost)
            PlayerEffects.applyVolumeBoost(e, savedBoost)
            PlayerSettings.defaultVolumeBoost = savedBoost
            bookRepository.markPlayed(bookId)
            _state.value =
                PlayerStateReducer.reduce(
                    _state.value,
                    PlayerEvent.VolumeBoostChanged(savedBoost),
                )
            // Пресет эквалайзера: сервис применит его к audio session, когда аудио будет готово.
            val savedPreset =
                runCatching { scanSettings.equalizerPreset.first() }
                    .getOrNull()
                    ?.let { name -> EqualizerPreset.entries.firstOrNull { it.name == name } }
            PlayerSettings.currentEqualizerPreset = savedPreset
            _state.value =
                PlayerStateReducer.reduce(
                    _state.value,
                    PlayerEvent.EqualizerChanged(savedPreset),
                )
            _state.value =
                PlayerStateReducer.reduce(
                    _state.value,
                    PlayerEvent.BookLoaded(
                        bookId,
                        book.title,
                        book.author,
                        book.coverPath,
                        book.totalDurationMs,
                        chapter,
                        book.filesPresent,
                        globalDurationMs = book.totalDurationMs,
                    ),
                )
            if (PlayerSettings.autoResume) {
                e.play()
                _state.value =
                    PlayerStateReducer.reduce(_state.value, PlayerEvent.IsPlayingChanged(true))
            } else {
                _state.value =
                    PlayerStateReducer.reduce(_state.value, PlayerEvent.IsPlayingChanged(false))
            }
            startPolling()
        }

        suspend fun pause() {
            val e = engine ?: return
            val bookId = currentBookId ?: return
            e.pause()
            saver.stop(bookId, e.positionMs, e.chapterIndex, currentDurationMs)
            recordSessionPoint(bookId, e.positionMs, e.chapterIndex)
            _state.value = PlayerStateReducer.reduce(_state.value, PlayerEvent.IsPlayingChanged(false))
        }

        fun resume() {
            // Если возобновили быстро — короткая пауза, отдельной сессии не было: убираем точку.
            val pending = pendingSessionId
            val pausedAt = lastPauseAt
            if (pending != null && System.currentTimeMillis() - pausedAt < SESSION_MERGE_MS) {
                scope?.launch { runCatching { bookmarkRepository.delete(pending) } }
            }
            pendingSessionId = null
            engine?.play()
        }

        fun seekTo(positionMs: Long) {
            // Перемотка в пределах текущей главы: НЕ переключаем главу, чтобы таймлайн
            // был предсказуемым и не «прыгал» на следующую главу при тапе.
            val e = engine ?: return
            e.seekTo(e.chapterIndex, positionMs.coerceIn(0L, e.durationMs.coerceAtLeast(0L)))
        }

        fun seekBack(stepMs: Long = PlayerSettings.seekStepMs) {
            engine?.seekBack(stepMs)
        }

        fun seekForward(stepMs: Long = PlayerSettings.seekStepMs) {
            engine?.seekForward(stepMs)
        }

        fun nextChapter() {
            engine?.nextChapter()
        }

        fun previousChapter() {
            engine?.previousChapter()
        }

        fun setSpeed(speed: Float) {
            val e = engine ?: return
            val clamped = speed.coerceIn(0.5f, 4.0f)
            PlayerEffects.applySpeed(e, clamped)
            // Запоминаем для следующих книг и переживаем перезапуск приложения.
            PlayerSettings.defaultSpeed = clamped
            settingsScope.launch { runCatching { scanSettings.setPlaybackSpeed(clamped) } }
            _state.value = PlayerStateReducer.reduce(_state.value, PlayerEvent.SpeedChanged(clamped))
        }

        fun setVolumeBoost(boost: Float) {
            val e = engine ?: return
            val clamped = boost.coerceIn(1.0f, 2.0f)
            Log.i("AudioBoost", "PlayerController.setVolumeBoost boost=$boost clamped=$clamped, VolumeBoost.gain -> $clamped")
            PlayerEffects.applyVolumeBoost(e, clamped)
            // Запоминаем для следующих книг и переживаем перезапуск приложения.
            PlayerSettings.defaultVolumeBoost = clamped
            settingsScope.launch { runCatching { scanSettings.setVolumeBoost(clamped) } }
            _state.value = PlayerStateReducer.reduce(_state.value, PlayerEvent.VolumeBoostChanged(clamped))
        }

        fun setEqualizer(preset: EqualizerPreset) {
            val e = engine ?: return
            EqualizerController.apply(e.audioSessionId(), preset)
            // Запоминаем пресет: сервис применит его при старте следующей книги.
            PlayerSettings.currentEqualizerPreset = preset
            settingsScope.launch {
                runCatching {
                    scanSettings.setEqualizerPreset(if (preset == EqualizerPreset.FLAT) null else preset.name)
                }
            }
            _state.value = PlayerStateReducer.reduce(_state.value, PlayerEvent.EqualizerChanged(preset))
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
                    kind = BookmarkKind.MANUAL,
                    chapterIndex = e.chapterIndex,
                )
            bookmarkRepository.add(bm)
            return bm
        }

        /** Создаёт авто-точку сессии на паузе (см. pendingSessionId/lastPauseAt). */
        private suspend fun recordSessionPoint(
            bookId: String,
            positionMs: Long,
            chapterIndex: Int,
        ) {
            val id = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val bm =
                Bookmark(
                    id = id,
                    bookId = bookId,
                    positionMs = positionMs,
                    title = "Сессия · " + sessionDateFormat.format(java.util.Date(now)),
                    kind = BookmarkKind.SESSION,
                    chapterIndex = chapterIndex,
                    createdAt = now,
                )
            runCatching { bookmarkRepository.add(bm) }
            pendingSessionId = id
            lastPauseAt = now
        }

        suspend fun deleteBookmark(id: String) = bookmarkRepository.delete(id)

        /** Одна итерация опроса: обновляет состояние, прогресс, таймер сна. */
        suspend fun tick(nowMs: Long = System.currentTimeMillis()) {
            val e = engine ?: return
            _state.value = PlayerStateReducer.reduce(_state.value, PlayerEvent.IsPlayingChanged(e.isPlaying))
            // Слайдер работает в пределах текущей главы (positionMs/durationMs — по главе),
            // чтобы перемотка не переключала главу. Глобальный прогресс — отдельный индикатор.
            _state.value = PlayerStateReducer.reduce(_state.value, PlayerEvent.PositionChanged(e.positionMs))
            _state.value = PlayerStateReducer.reduce(_state.value, PlayerEvent.DurationChanged(e.durationMs))
            _state.value =
                PlayerStateReducer.reduce(
                    _state.value,
                    PlayerEvent.GlobalPositionChanged(chapterToGlobal(e.chapterIndex, e.positionMs)),
                )
            _state.value = PlayerStateReducer.reduce(_state.value, PlayerEvent.ChapterChanged(e.chapterIndex))
            val bookId = currentBookId ?: return
            // Прогресс в БД по-прежнему хранится по главам (позиция внутри главы + индекс главы),
            // чтобы восстановление воспроизведения оставалось корректным.
            val dur = if (e.durationMs > 0) e.durationMs else currentDurationMs
            saver.tick(bookId, e.positionMs, e.chapterIndex, dur, nowMs)
            if (!e.isPlaying && dur > 0 && e.positionMs >= dur - 1_500L && !completedNotified) {
                completedNotified = true
                bookRepository.markCompleted(bookId)
            }
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

        /** Накопленные смещения начала каждой главы: offsets[i] = Σ длительностей глав < i. */
        private fun buildOffsets(chapters: List<Chapter>): LongArray {
            val offsets = LongArray(chapters.size)
            var acc = 0L
            for (i in chapters.indices) {
                offsets[i] = acc
                acc += chapters[i].endMs.coerceAtLeast(0L)
            }
            return offsets
        }

        /** Глобальная позиция книги по (глава, позиция внутри главы). */
        private fun chapterToGlobal(
            chapterIndex: Int,
            positionMs: Long,
        ): Long {
            val i = chapterIndex.coerceIn(0, (chapterOffsets.size - 1).coerceAtLeast(0))
            return (chapterOffsets.getOrElse(i) { 0L } + positionMs).coerceAtLeast(0L)
        }
    }

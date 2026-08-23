package com.nikit.audiobook.player.controller

import com.nikit.audiobook.player.effects.EqualizerPreset

/** Состояние плеера для UI. */
data class PlayerUiState(
    val bookId: String? = null,
    val title: String? = null,
    val author: String? = null,
    val coverPath: String? = null,
    val chapterIndex: Int = 0,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val speed: Float = 1f,
    val volumeBoost: Float = 1f,
    val equalizerPreset: EqualizerPreset? = null,
    val sleepLeftMs: Long? = null,
    val filesPresent: Boolean = true,
    // Глобальный (сквозной по книге) прогресс — только для индикатора, не для слайдера.
    val globalPositionMs: Long = 0L,
    val globalDurationMs: Long = 0L,
)

/** События от плеера → обновление состояния. */
sealed class PlayerEvent {
    data class BookLoaded(
        val bookId: String,
        val title: String,
        val author: String? = null,
        val coverPath: String? = null,
        val durationMs: Long,
        val chapterIndex: Int = 0,
        val filesPresent: Boolean = true,
        val globalDurationMs: Long = 0L,
    ) : PlayerEvent()

    data class IsPlayingChanged(
        val playing: Boolean,
    ) : PlayerEvent()

    data class PositionChanged(
        val positionMs: Long,
    ) : PlayerEvent()

    data class ChapterChanged(
        val index: Int,
    ) : PlayerEvent()

    data class DurationChanged(
        val durationMs: Long,
    ) : PlayerEvent()

    data class GlobalPositionChanged(
        val positionMs: Long,
    ) : PlayerEvent()

    data class SpeedChanged(
        val speed: Float,
    ) : PlayerEvent()

    data class VolumeBoostChanged(
        val boost: Float,
    ) : PlayerEvent()

    data class EqualizerChanged(
        val preset: EqualizerPreset?,
    ) : PlayerEvent()

    data class SleepLeftChanged(
        val msLeft: Long?,
    ) : PlayerEvent()
}

/** Чистый редьюсер состояния плеера. */
object PlayerStateReducer {
    fun reduce(
        state: PlayerUiState,
        event: PlayerEvent,
    ): PlayerUiState =
        when (event) {
            is PlayerEvent.BookLoaded -> {
                state.copy(
                    bookId = event.bookId,
                    title = event.title,
                    author = event.author,
                    coverPath = event.coverPath,
                    durationMs = 0L,
                    globalDurationMs = event.globalDurationMs,
                    chapterIndex = event.chapterIndex,
                    positionMs = 0L,
                    filesPresent = event.filesPresent,
                )
            }

            is PlayerEvent.IsPlayingChanged -> {
                state.copy(isPlaying = event.playing)
            }

            is PlayerEvent.PositionChanged -> {
                state.copy(positionMs = event.positionMs)
            }

            is PlayerEvent.ChapterChanged -> {
                state.copy(chapterIndex = event.index)
            }

            is PlayerEvent.DurationChanged -> {
                state.copy(durationMs = event.durationMs)
            }

            is PlayerEvent.GlobalPositionChanged -> {
                state.copy(globalPositionMs = event.positionMs)
            }

            is PlayerEvent.SpeedChanged -> {
                state.copy(speed = event.speed)
            }

            is PlayerEvent.VolumeBoostChanged -> {
                state.copy(volumeBoost = event.boost)
            }

            is PlayerEvent.EqualizerChanged -> {
                state.copy(equalizerPreset = event.preset)
            }

            is PlayerEvent.SleepLeftChanged -> {
                state.copy(sleepLeftMs = event.msLeft)
            }
        }
}

package com.nikit.audiobook.player.controller

/** Состояние плеера для UI. */
data class PlayerUiState(
    val bookId: String? = null,
    val title: String? = null,
    val chapterIndex: Int = 0,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val speed: Float = 1f,
    val volumeBoost: Float = 1f,
    val sleepLeftMs: Long? = null,
    val filesPresent: Boolean = true,
)

/** События от плеера → обновление состояния. */
sealed class PlayerEvent {
    data class BookLoaded(
        val bookId: String,
        val title: String,
        val durationMs: Long,
        val chapterIndex: Int = 0,
        val filesPresent: Boolean = true,
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

    data class SpeedChanged(
        val speed: Float,
    ) : PlayerEvent()

    data class VolumeBoostChanged(
        val boost: Float,
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
                    durationMs = event.durationMs,
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

            is PlayerEvent.SpeedChanged -> {
                state.copy(speed = event.speed)
            }

            is PlayerEvent.VolumeBoostChanged -> {
                state.copy(volumeBoost = event.boost)
            }

            is PlayerEvent.SleepLeftChanged -> {
                state.copy(sleepLeftMs = event.msLeft)
            }
        }
}

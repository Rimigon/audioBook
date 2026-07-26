package com.nikit.audiobook.player.sleep

/** Решение таймера сна на очередном тике. */
sealed class SleepDecision {
    data class Continue(
        val msLeft: Long,
    ) : SleepDecision()

    object Pause : SleepDecision()
}

/** Режим таймера сна. */
sealed class SleepMode {
    data class Duration(
        val endAt: Long,
    ) : SleepMode()

    data class UntilEndOfChapter(
        val chapterEndAt: Long,
    ) : SleepMode()
}

/**
 * Чистая логика таймера сна. Не зависит от Android/ExoPlayer.
 * Часы инъектируются ([clock]) для тестирования.
 */
class SleepTimer(
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private var mode: SleepMode? = null

    val isRunning: Boolean get() = mode != null

    fun startDuration(durationMs: Long) {
        mode = SleepMode.Duration(clock() + durationMs)
    }

    fun startUntilChapterEnd(remainingChapterMs: Long) {
        mode = SleepMode.UntilEndOfChapter(clock() + remainingChapterMs)
    }

    fun cancel() {
        mode = null
    }

    /** Вызывается на каждом тике. При срабатывании возвращает [SleepDecision.Pause] и сбрасывает режим. */
    fun tick(): SleepDecision {
        val m = mode ?: return SleepDecision.Continue(0L)
        val now = clock()
        val endAt =
            when (m) {
                is SleepMode.Duration -> m.endAt
                is SleepMode.UntilEndOfChapter -> m.chapterEndAt
            }
        val left = endAt - now
        return if (left <= 0L) {
            mode = null
            SleepDecision.Pause
        } else {
            SleepDecision.Continue(left)
        }
    }
}

package com.nikit.audiobook.player.progress

import com.nikit.audiobook.data.repo.ProgressRepository
import com.nikit.audiobook.domain.model.PlaybackProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Координатор сохранения прогресса воспроизведения.
 * Опрашивает позицию плеера через [positionProvider] (мс) и пишет в [ProgressRepository]
 * не чаще, чем раз в [intervalMs]; при [stop] форсирует финальное сохранение.
 */
class ProgressSaverCoordinator(
    private val progressRepository: ProgressRepository,
    private val saver: ProgressSaver = ProgressSaver(),
    private val intervalMs: Long = 1_000L,
) {
    private var lastSaveMs: Long = 0L
    private var everSaved: Boolean = false
    private var currentBookId: String? = null

    suspend fun tick(
        bookId: String,
        positionMs: Long,
        chapterIndex: Int,
        durationMs: Long,
        nowMs: Long,
    ) {
        currentBookId = bookId
        if (!everSaved || saver.shouldSave(lastSaveMs, nowMs)) {
            save(bookId, positionMs, chapterIndex, durationMs)
            lastSaveMs = nowMs
            everSaved = true
        }
    }

    suspend fun stop(
        positionMs: Long,
        chapterIndex: Int,
        durationMs: Long,
    ) {
        val bookId = currentBookId ?: return
        save(bookId, positionMs, chapterIndex, durationMs)
    }

    private suspend fun save(
        bookId: String,
        positionMs: Long,
        chapterIndex: Int,
        durationMs: Long,
    ) = withContext(Dispatchers.IO) {
        val percent = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
        progressRepository.upsert(
            PlaybackProgress(
                bookId = bookId,
                positionMs = positionMs,
                chapterIndex = chapterIndex,
                percent = percent,
                lastPlayedAt = System.currentTimeMillis(),
            ),
        )
    }

    /** Периодический поток позиций раз в [intervalMs] — helper для сервиса. */
    fun ticker(positionProvider: suspend () -> Long): Flow<Long> =
        flow {
            while (true) {
                emit(positionProvider())
                delay(intervalMs)
            }
        }
}

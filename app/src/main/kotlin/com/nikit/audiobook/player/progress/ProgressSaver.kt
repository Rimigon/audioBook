package com.nikit.audiobook.player.progress

/** Чистая throttle-логика: сохранять прогресс не чаще, чем раз в [intervalMs]. */
class ProgressSaver(
    private val intervalMs: Long = 10_000L,
) {
    fun shouldSave(
        lastSaveMs: Long,
        nowMs: Long,
    ): Boolean = (nowMs - lastSaveMs) >= intervalMs
}

package com.nikit.audiobook.player.controller

import com.nikit.audiobook.player.effects.PlayerHandle

/** Движок воспроизведения (обёртка над Media3 MediaController). Тестируем через фейк. */
interface PlayerEngine : PlayerHandle {
    val positionMs: Long
    val durationMs: Long
    val chapterIndex: Int
    val isPlaying: Boolean
    val hasNextChapter: Boolean
    val hasPreviousChapter: Boolean

    fun setMediaItems(uris: List<String>)

    fun seekTo(
        chapterIndex: Int,
        positionMs: Long,
    )

    fun play()

    fun pause()

    fun seekBack(ms: Long)

    fun seekForward(ms: Long)

    fun nextChapter()

    fun previousChapter()
}

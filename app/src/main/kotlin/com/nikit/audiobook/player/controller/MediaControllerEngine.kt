package com.nikit.audiobook.player.controller

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.session.MediaController

/**
 * Реальная обёртка над Media3 [MediaController] (UI-процесс).
 * Эквалайзер недоступен через MediaController (нужен audioSessionId из ExoPlayer в сервисе) —
 * это задокументированное ограничение: эквалайзер применяется только при возможности получить
 * audio session id (например, через отдельный канал в будущем). audioSessionId() возвращает 0.
 */
class MediaControllerEngine(
    private val controller: MediaController,
) : PlayerEngine {
    override val positionMs: Long get() = controller.currentPosition
    override val durationMs: Long get() = controller.duration
    override val chapterIndex: Int get() = controller.currentMediaItemIndex
    override val isPlaying: Boolean get() = controller.isPlaying

    override fun setMediaItems(uris: List<String>) {
        controller.setMediaItems(uris.map { MediaItem.fromUri(it) })
    }

    override fun seekTo(
        chapterIndex: Int,
        positionMs: Long,
    ) {
        controller.seekTo(chapterIndex, positionMs)
    }

    override fun play() = controller.play()

    override fun pause() = controller.pause()

    override fun setPlaybackSpeed(speed: Float) {
        controller.playbackParameters = PlaybackParameters(speed, 1f)
    }

    override fun setVolume(volume: Float) {
        controller.volume = volume
    }

    override fun audioSessionId(): Int = 0
}

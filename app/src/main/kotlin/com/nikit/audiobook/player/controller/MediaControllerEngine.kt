package com.nikit.audiobook.player.controller

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.session.MediaController

class MediaControllerEngine(
    private val controller: MediaController,
) : PlayerEngine {
    override val positionMs: Long get() = controller.currentPosition
    override val durationMs: Long get() = controller.duration
    override val chapterIndex: Int get() = controller.currentMediaItemIndex
    override val isPlaying: Boolean get() = controller.isPlaying
    override val hasNextChapter: Boolean get() = controller.hasNextMediaItem()
    override val hasPreviousChapter: Boolean get() = controller.hasPreviousMediaItem()

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

    override fun seekBack(ms: Long) {
        val target = (controller.currentPosition - ms).coerceAtLeast(0L)
        controller.seekTo(target)
    }

    override fun seekForward(ms: Long) {
        val dur = controller.duration.coerceAtLeast(0L)
        val target =
            if (dur > 0) {
                (controller.currentPosition + ms).coerceAtMost(dur)
            } else {
                controller.currentPosition + ms
            }
        controller.seekTo(target)
    }

    override fun nextChapter() = controller.seekToNextMediaItem()

    override fun previousChapter() = controller.seekToPreviousMediaItem()

    override fun setPlaybackSpeed(speed: Float) {
        controller.playbackParameters = PlaybackParameters(speed, 1f)
    }

    override fun setVolume(volume: Float) {
        controller.volume = volume
    }

    override fun audioSessionId(): Int = 0
}

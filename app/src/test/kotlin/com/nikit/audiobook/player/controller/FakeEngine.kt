package com.nikit.audiobook.player.controller

internal class FakeEngine : PlayerEngine {
    var _items: List<String> = emptyList()
    var _playing: Boolean = false
    var _pos: Long = 0L
    var _dur: Long = 100_000L
    var _chapter: Int = 0
    var _speed: Float = 1f
    var _volume: Float = 1f

    override val positionMs: Long get() = _pos
    override val durationMs: Long get() = _dur
    override val chapterIndex: Int get() = _chapter
    override val isPlaying: Boolean get() = _playing
    override val hasNextChapter: Boolean get() = _chapter < _items.size - 1
    override val hasPreviousChapter: Boolean get() = _chapter > 0

    override fun setMediaItems(uris: List<String>) {
        _items = uris
    }

    override fun seekTo(
        chapterIndex: Int,
        positionMs: Long,
    ) {
        _chapter = chapterIndex
        _pos = positionMs
    }

    override fun play() {
        _playing = true
    }

    override fun pause() {
        _playing = false
    }

    override fun seekBack(ms: Long) {
        _pos = (_pos - ms).coerceAtLeast(0L)
    }

    override fun seekForward(ms: Long) {
        _pos = (_pos + ms).coerceAtMost(_dur)
    }

    override fun nextChapter() {
        if (hasNextChapter) _chapter++
    }

    override fun previousChapter() {
        if (hasPreviousChapter) _chapter--
    }

    override fun setPlaybackSpeed(speed: Float) {
        _speed = speed
    }

    override fun setVolume(volume: Float) {
        _volume = volume
    }

    override fun audioSessionId(): Int = 0
}

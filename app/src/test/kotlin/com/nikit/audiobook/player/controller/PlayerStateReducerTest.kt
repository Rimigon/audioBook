package com.nikit.audiobook.player.controller

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlayerStateReducerTest {
    @Test fun loadBookThenPlayUpdates() {
        var s = PlayerUiState()
        s = PlayerStateReducer.reduce(s, PlayerEvent.BookLoaded("b1", "Дюна", 3600_000, 0, true))
        s = PlayerStateReducer.reduce(s, PlayerEvent.IsPlayingChanged(true))
        assertThat(s.bookId).isEqualTo("b1")
        assertThat(s.title).isEqualTo("Дюна")
        assertThat(s.isPlaying).isTrue()
        assertThat(s.durationMs).isEqualTo(3600_000L)
    }

    @Test fun positionAndChapterUpdate() {
        var s = PlayerUiState(bookId = "b1")
        s = PlayerStateReducer.reduce(s, PlayerEvent.PositionChanged(12_000L))
        s = PlayerStateReducer.reduce(s, PlayerEvent.ChapterChanged(3))
        assertThat(s.positionMs).isEqualTo(12_000L)
        assertThat(s.chapterIndex).isEqualTo(3)
    }

    @Test fun speedAndVolumePersist() {
        var s = PlayerUiState()
        s = PlayerStateReducer.reduce(s, PlayerEvent.SpeedChanged(2f))
        s = PlayerStateReducer.reduce(s, PlayerEvent.VolumeBoostChanged(1.5f))
        assertThat(s.speed).isEqualTo(2f)
        assertThat(s.volumeBoost).isEqualTo(1.5f)
    }

    @Test fun sleepLeftNullable() {
        var s = PlayerUiState()
        s = PlayerStateReducer.reduce(s, PlayerEvent.SleepLeftChanged(300_000L))
        assertThat(s.sleepLeftMs).isEqualTo(300_000L)
        s = PlayerStateReducer.reduce(s, PlayerEvent.SleepLeftChanged(null))
        assertThat(s.sleepLeftMs).isNull()
    }
}

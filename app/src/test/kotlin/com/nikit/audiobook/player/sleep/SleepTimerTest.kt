package com.nikit.audiobook.player.sleep

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SleepTimerTest {
    private var now = 0L
    private val timer = SleepTimer { now }

    @Test fun durationCountsDownToPause() {
        timer.startDuration(60_000)
        now = 30_000
        val mid = timer.tick()
        assertThat(mid).isInstanceOf(SleepDecision.Continue::class.java)
        assertThat((mid as SleepDecision.Continue).msLeft).isEqualTo(30_000)
        now = 61_000
        val end = timer.tick()
        assertThat(end).isEqualTo(SleepDecision.Pause)
        assertThat(timer.isRunning).isFalse()
    }

    @Test fun untilEndOfChapterPausesWhenRemainingZero() {
        timer.startUntilChapterEnd(20_000)
        now = 19_000
        assertThat(timer.tick()).isInstanceOf(SleepDecision.Continue::class.java)
        now = 20_000
        assertThat(timer.tick()).isEqualTo(SleepDecision.Pause)
    }

    @Test fun cancelStops() {
        timer.startDuration(5000)
        timer.cancel()
        assertThat(timer.isRunning).isFalse()
        assertThat(timer.tick()).isInstanceOf(SleepDecision.Continue::class.java)
    }

    @Test fun idleWhenNotStarted() {
        assertThat(timer.isRunning).isFalse()
        val d = timer.tick()
        assertThat(d).isInstanceOf(SleepDecision.Continue::class.java)
        assertThat((d as SleepDecision.Continue).msLeft).isEqualTo(0L)
    }
}

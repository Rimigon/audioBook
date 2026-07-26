package com.nikit.audiobook.player.effects

import com.google.common.truth.Truth.assertThat
import org.junit.Test

private class FakePlayer : PlayerHandle {
    var speed: Float = 1f
    var vol: Float = 1f

    override fun setPlaybackSpeed(speed: Float) {
        this.speed = speed
    }

    override fun setVolume(volume: Float) {
        this.vol = volume
    }

    override fun audioSessionId(): Int = 0
}

class PlayerEffectsTest {
    @Test fun speedClampedToRange() {
        val p = FakePlayer()
        PlayerEffects.applySpeed(p, 10f)
        assertThat(p.speed).isEqualTo(4.0f)
        PlayerEffects.applySpeed(p, 0.1f)
        assertThat(p.speed).isEqualTo(0.5f)
        PlayerEffects.applySpeed(p, 1.75f)
        assertThat(p.speed).isEqualTo(1.75f)
    }

    @Test fun volumeBoostClamped() {
        val p = FakePlayer()
        PlayerEffects.applyVolumeBoost(p, 5f)
        assertThat(p.vol).isEqualTo(2.0f)
        PlayerEffects.applyVolumeBoost(p, 0.5f)
        assertThat(p.vol).isEqualTo(1.0f)
        PlayerEffects.applyVolumeBoost(p, 1.5f)
        assertThat(p.vol).isEqualTo(1.5f)
    }

    @Test fun equalizerNoSessionReturnsNull() {
        assertThat(PlayerEffects.applyEqualizer(0, EqualizerPreset.FLAT)).isNull()
    }
}

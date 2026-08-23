package com.nikit.audiobook.player.effects

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test

private open class FakePlayer : PlayerHandle {
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

/** Повторяет поведение Media3 MediaController.setVolume: IllegalArgumentException при volume > 1. */
private class StrictMedia3Player : FakePlayer() {
    override fun setVolume(volume: Float) {
        require(volume in 0f..1f) { "volume must be between 0 and 1" }
        super.setVolume(volume)
    }
}

class PlayerEffectsTest {
    @After
    fun resetGain() {
        VolumeBoost.gain = 1f
    }

    @Test fun speedClampedToRange() {
        val p = FakePlayer()
        PlayerEffects.applySpeed(p, 10f)
        assertThat(p.speed).isEqualTo(4.0f)
        PlayerEffects.applySpeed(p, 0.1f)
        assertThat(p.speed).isEqualTo(0.5f)
        PlayerEffects.applySpeed(p, 1.75f)
        assertThat(p.speed).isEqualTo(1.75f)
    }

    @Test
    fun volumeBoostKeepsPlayerVolumeInMedia3RangeAndSetsGain() {
        val p = FakePlayer()

        PlayerEffects.applyVolumeBoost(p, 5f)
        assertThat(p.vol).isEqualTo(1.0f) // Media3 кидает IllegalArgumentException при volume > 1
        assertThat(VolumeBoost.gain).isEqualTo(2.0f)

        PlayerEffects.applyVolumeBoost(p, 0.5f)
        assertThat(p.vol).isEqualTo(1.0f)
        assertThat(VolumeBoost.gain).isEqualTo(1.0f)

        PlayerEffects.applyVolumeBoost(p, 1.5f)
        assertThat(p.vol).isEqualTo(1.0f)
        assertThat(VolumeBoost.gain).isEqualTo(1.5f)
    }

    @Test
    fun allBoostPresetsDoNotCrashStrictMedia3Player() {
        val p = StrictMedia3Player()
        for (preset in listOf(1.0f, 1.25f, 1.5f, 2.0f)) {
            PlayerEffects.applyVolumeBoost(p, preset) // не должно бросить исключение
        }
        assertThat(p.vol).isEqualTo(1.0f)
        assertThat(VolumeBoost.gain).isEqualTo(2.0f)
    }

    @Test fun equalizerNoSessionReturnsNull() {
        assertThat(PlayerEffects.applyEqualizer(0, EqualizerPreset.FLAT)).isNull()
    }
}

package com.nikit.audiobook.player.effects

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

@OptIn(UnstableApi::class)
class GainAudioProcessorTest {
    @After
    fun resetGain() {
        VolumeBoost.gain = 1f
    }

    private fun pcm16(vararg samples: Short): ByteBuffer =
        ByteBuffer.allocateDirect(samples.size * 2).order(ByteOrder.nativeOrder()).apply {
            samples.forEach { putShort(it) }
            flip()
        }

    private fun pcmFloat(vararg samples: Float): ByteBuffer =
        ByteBuffer.allocateDirect(samples.size * 4).order(ByteOrder.nativeOrder()).apply {
            samples.forEach { putFloat(it) }
            flip()
        }

    @Test
    fun inactiveForNonPcmEncoding() {
        val p = GainAudioProcessor()
        val out = p.configure(AudioFormat(44_100, 2, C.ENCODING_PCM_8BIT))
        assertThat(out).isSameInstanceAs(AudioFormat.NOT_SET)
        assertThat(p.isActive).isFalse()
    }

    @Test
    fun activeForPcm16AndAmplifies() {
        VolumeBoost.gain = 2f
        val p = GainAudioProcessor()
        p.configure(AudioFormat(44_100, 2, C.ENCODING_PCM_16BIT))
        p.flush()
        assertThat(p.isActive).isTrue()

        p.queueInput(pcm16(1000, -2000))
        val out = p.getOutput()
        assertThat(out.short).isEqualTo(2000.toShort())
        assertThat(out.short).isEqualTo((-4000).toShort())
        assertThat(out.hasRemaining()).isFalse()
    }

    @Test
    fun gainOneIsIdentity() {
        VolumeBoost.gain = 1f
        val p = GainAudioProcessor()
        p.configure(AudioFormat(44_100, 2, C.ENCODING_PCM_16BIT))
        p.flush()

        p.queueInput(pcm16(1234, -5678))
        val out = p.getOutput()
        assertThat(out.short).isEqualTo(1234.toShort())
        assertThat(out.short).isEqualTo((-5678).toShort())
    }

    @Test
    fun pcm16ClampedToShortRange() {
        VolumeBoost.gain = 2f
        val p = GainAudioProcessor()
        p.configure(AudioFormat(44_100, 1, C.ENCODING_PCM_16BIT))
        p.flush()

        p.queueInput(pcm16(20_000, -20_000))
        val out = p.getOutput()
        assertThat(out.short).isEqualTo(Short.MAX_VALUE)
        assertThat(out.short).isEqualTo(Short.MIN_VALUE)
    }

    @Test
    fun amplifiesPcmFloat() {
        VolumeBoost.gain = 1.5f
        val p = GainAudioProcessor()
        p.configure(AudioFormat(44_100, 2, C.ENCODING_PCM_FLOAT))
        p.flush()

        p.queueInput(pcmFloat(0.5f, -0.25f))
        val out = p.getOutput()
        assertThat(out.float).isEqualTo(0.75f)
        assertThat(out.float).isEqualTo(-0.375f)
    }
}

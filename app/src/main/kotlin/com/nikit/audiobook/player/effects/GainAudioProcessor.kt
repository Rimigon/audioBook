package com.nikit.audiobook.player.effects

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.round

/**
 * Программное усиление PCM-сэмплов (буст громкости 1.0–2.0).
 *
 * Media3 `MediaController.setVolume` валидирует volume ∈ [0, 1] и бросает
 * `IllegalArgumentException` при значении >1, поэтому усиление выше 1.0
 * принципиально нельзя сделать через `player.setVolume`. Вместо этого
 * процессор ставится в цепочку аудио-обработки ExoPlayer через
 * `DefaultAudioSink.Builder.setAudioProcessors(...)` и умножает сэмплы
 * на [VolumeBoost.gain] перед записью в AudioTrack.
 *
 * Громкость плеера (player.volume, 0..1) остаётся в допустимом диапазоне
 * и перемножается с этим усилением в DefaultAudioSink: итог = volume × gain.
 */
@OptIn(UnstableApi::class)
class GainAudioProcessor : BaseAudioProcessor() {
    /** Последний gain, применённый к реальному аудио (для диагностики, см. AudioBoost). */
    @Volatile
    private var lastAppliedGain: Float = 1f

    // Диагностика амплитуды: max|s| входного и выходного PCM (логируется ~раз в секунду при gain > 1).
    private var inMax: Int = 0
    private var outMax: Int = 0
    private var lastProbeAt: Long = 0L

    private fun diag(message: String) {
        // runCatching: в JVM-юнит-тестах android.util.Log бросает «not mocked».
        runCatching { Log.i("AudioBoost", message) }
    }

    private fun probeAmplitude(gain: Float) {
        val now = System.currentTimeMillis()
        if (now - lastProbeAt < 1_000L) return
        lastProbeAt = now
        diag(
            "GainAudioProcessor.amplitude gain=$gain " +
                "inMaxAbs=$inMax outMaxAbs=$outMax " +
                "(in=~${(inMax / 327.67).toInt()}% dBFS, out=~${(outMax / 327.67).toInt()}% dBFS)",
        )
        inMax = 0
        outMax = 0
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val out =
            when (inputAudioFormat.encoding) {
                C.ENCODING_PCM_16BIT, C.ENCODING_PCM_FLOAT -> inputAudioFormat
                else -> AudioProcessor.AudioFormat.NOT_SET // чужой формат — прозрачный проход
            }
        diag(
            "GainAudioProcessor.configure encoding=${inputAudioFormat.encoding} " +
                "sr=${inputAudioFormat.sampleRate} ch=${inputAudioFormat.channelCount} -> " +
                if (out == AudioProcessor.AudioFormat.NOT_SET) "INACTIVE" else "ACTIVE",
        )
        return out
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val position = inputBuffer.position()
        val limit = inputBuffer.limit()
        if (position == limit) return

        val gain = VolumeBoost.gain.coerceIn(1.0f, 2.0f)
        // Диагностика: логируем смену gain, чтобы видеть, доезжает ли он до реального аудио.
        if (gain != lastAppliedGain) {
            lastAppliedGain = gain
            diag("GainAudioProcessor.queueInput gain=$gain (encoding=${inputAudioFormat.encoding})")
        }
        val output = replaceOutputBuffer(limit - position)
        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> while (inputBuffer.position() < limit) {
                val s = inputBuffer.short
                val boosted = applyGain(s, gain)
                if (gain != 1.0f) {
                    val aIn = if (s < 0) -s.toInt() else s.toInt()
                    val aOut = if (boosted < 0) -boosted.toInt() else boosted.toInt()
                    if (aIn > inMax) inMax = aIn
                    if (aOut > outMax) outMax = aOut
                }
                output.putShort(boosted)
            }

            C.ENCODING_PCM_FLOAT -> while (inputBuffer.position() < limit) {
                output.putFloat((inputBuffer.float * gain).coerceIn(-1.0f, 1.0f))
            }

            else -> Unit // не достижимо: onConfigure вернул NOT_SET для чужих кодировок
        }
        inputBuffer.position(limit)
        output.flip()
        probeAmplitude(gain)
    }

    private fun applyGain(
        sample: Short,
        gain: Float,
    ): Short {
        if (gain == 1.0f) return sample
        val scaled = round(sample * gain)
        return when {
            scaled >= Short.MAX_VALUE -> Short.MAX_VALUE
            scaled <= Short.MIN_VALUE -> Short.MIN_VALUE
            else -> scaled.toInt().toShort()
        }
    }
}

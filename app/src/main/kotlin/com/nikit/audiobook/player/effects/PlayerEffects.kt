package com.nikit.audiobook.player.effects

/** Минимальный контракт над ExoPlayer для применения эффектов (тестируемый). */
interface PlayerHandle {
    fun setPlaybackSpeed(speed: Float)

    fun setVolume(volume: Float)

    fun audioSessionId(): Int
}

/** Эквалайзер-пресеты. */
enum class EqualizerPreset { FLAT, BASS_BOOST, VOICE_CLARITY }

/** Применение эффектов к плееру. Часть (speed/volume) тестируется через фейк; eq — smoke. */
object PlayerEffects {
    /** Скорость 0.5–4.0, pitch не меняется. */
    fun applySpeed(
        player: PlayerHandle,
        speed: Float,
    ) {
        val clamped = speed.coerceIn(0.5f, 4.0f)
        player.setPlaybackSpeed(clamped)
    }

    /** Volume boost 1.0–2.0 (программное усиление выше 1.0). */
    fun applyVolumeBoost(
        player: PlayerHandle,
        boost: Float,
    ) {
        val clamped = boost.coerceIn(1.0f, 2.0f)
        player.setVolume(clamped)
    }

    /** Применяет эквалайзер к audio session. Возвращает открытый Equalizer (вызывающий держит ссылку). */
    fun applyEqualizer(
        audioSessionId: Int,
        preset: EqualizerPreset,
    ): android.media.audiofx.Equalizer? {
        if (audioSessionId == 0) return null
        return runCatching {
            val eq = android.media.audiofx.Equalizer(0, audioSessionId)
            val bands = eq.numberOfBands.toInt()
            when (preset) {
                EqualizerPreset.FLAT -> for (b in 0 until bands) eq.setBandLevel(b.toShort(), 0.toShort())

                EqualizerPreset.BASS_BOOST -> for (b in 0 until bands) {
                    val boost = (eq.bandLevelRange[1].toInt() / 2).toShort()
                    val level = if (b < bands / 2) boost else 0.toShort()
                    eq.setBandLevel(b.toShort(), level)
                }

                EqualizerPreset.VOICE_CLARITY -> for (b in 0 until bands) {
                    val high = (eq.bandLevelRange[1].toInt() / 3).toShort()
                    val level = if (b >= bands / 2) high else 0.toShort()
                    eq.setBandLevel(b.toShort(), level)
                }
            }
            eq.enabled = true
            eq
        }.getOrNull()
    }
}

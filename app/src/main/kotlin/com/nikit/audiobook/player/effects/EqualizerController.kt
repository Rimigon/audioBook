package com.nikit.audiobook.player.effects

import android.media.audiofx.Equalizer

/**
 * Управление жизненным циклом эквалайзера: держит открытый [Equalizer] и корректно
 * освобождает его при смене пресета или выключении. Без этого Android-эффекты
 * «висят» на audio session и копятся при каждом переключении.
 *
 * Низкоуровневое применение полос — [PlayerEffects.applyEqualizer].
 */
object EqualizerController {
    @Volatile
    private var eq: Equalizer? = null

    /**
     * Применяет пресет к audio session. FLAT/null — эквалайзер выключается (эффект освобождается).
     * При невалидном session id (0) — no-op.
     */
    fun apply(
        audioSessionId: Int,
        preset: EqualizerPreset?,
    ) {
        if (audioSessionId == 0) return
        release()
        if (preset == null || preset == EqualizerPreset.FLAT) return
        eq = PlayerEffects.applyEqualizer(audioSessionId, preset)
    }

    fun release() {
        eq?.release()
        eq = null
    }
}

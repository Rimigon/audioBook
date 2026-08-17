package com.nikit.audiobook.player.effects

/**
 * Текущее программное усиление громкости (1.0–2.0), общее для UI и сервиса воспроизведения.
 *
 * Сервис ([androidx.media3.session.MediaSessionService]) объявлен в том же процессе
 * (в манифесте нет `android:process`), поэтому статического холдера достаточно:
 * [PlayerEffects.applyVolumeBoost] пишет значение из UI-потока,
 * [GainAudioProcessor] читает его на playback-потоке ExoPlayer (поле volatile).
 */
object VolumeBoost {
    @Volatile
    var gain: Float = 1f
}

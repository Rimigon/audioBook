package com.nikit.audiobook.player.service

import android.content.Context
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.nikit.audiobook.player.controller.PlayerSettings
import com.nikit.audiobook.player.effects.EqualizerController
import com.nikit.audiobook.player.effects.GainAudioProcessor
import dagger.hilt.android.AndroidEntryPoint

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class AudioBookPlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()
        // Программное усиление громкости выше 1.0 (Media3 запрещает volume > 1):
        // кастомный AudioSink с GainAudioProcessor в цепочке обработки PCM.
        val audioSink =
            DefaultAudioSink
                .Builder(this)
                .setAudioProcessors(arrayOf(GainAudioProcessor()))
                .build()
        Log.i("AudioBoost", "AudioBookPlaybackService: custom sink with GainAudioProcessor created")
        val renderersFactory =
            object : DefaultRenderersFactory(this@AudioBookPlaybackService) {
                override fun buildAudioSink(
                    context: Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean,
                ): AudioSink = audioSink
            }
        player =
            ExoPlayer
                .Builder(this)
                .setRenderersFactory(renderersFactory)
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                        .build(),
                    // handleAudioFocus =
                    true,
                ).setHandleAudioBecomingNoisy(true)
                .setSeekBackIncrementMs(PlayerSettings.seekStepMs)
                .setSeekForwardIncrementMs(PlayerSettings.seekStepMs)
                .build()
        mediaSession = MediaSession.Builder(this, player).build()
        // Публикуем audio session id и применяем сохранённый пресет эквалайзера,
        // когда аудио реально готово (до этого момента session id = UNSET).
        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        PlayerSettings.audioSessionId = player.audioSessionId
                        PlayerSettings.currentEqualizerPreset?.let { preset ->
                            EqualizerController.apply(PlayerSettings.audioSessionId, preset)
                        }
                    }
                }
            },
        )
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        if (!player.playWhenReady) {
            player.pause()
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession
}

package com.nikit.audiobook.ui.player

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.nikit.audiobook.data.repo.BookRepository
import com.nikit.audiobook.player.controller.MediaControllerEngine
import com.nikit.audiobook.player.controller.PlayerController
import com.nikit.audiobook.player.controller.PlayerUiState
import com.nikit.audiobook.player.effects.EqualizerPreset
import com.nikit.audiobook.player.service.AudioBookPlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val playerController: PlayerController,
        private val bookRepository: BookRepository,
    ) : ViewModel() {
        val connected = MutableStateFlow(false)
        val state: StateFlow<PlayerUiState> = playerController.state
        private val connectionError = MutableStateFlow<String?>(null)

        init {
            connect()
        }

        private fun connect() {
            val token = SessionToken(context, ComponentName(context, AudioBookPlaybackService::class.java))
            val future = MediaController.Builder(context, token).buildAsync()
            future.addListener({
                try {
                    val controller = future.get()
                    playerController.attach(MediaControllerEngine(controller))
                    connected.value = true
                    connectionError.value = null
                } catch (e: Exception) {
                    Log.e("PlayerViewModel", "MediaController connection failed", e)
                    connectionError.value = e.message ?: e::class.java.simpleName
                }
            }, ContextCompat.getMainExecutor(context))
        }

        /** Перезапустить воспроизведение книги, дождавшись подключения к сервису. */
        fun playBook(
            bookId: String,
            chapterIndex: Int? = null,
            startPositionMs: Long? = null,
        ) = viewModelScope.launch {
            awaitConnection()
            playerController.loadBook(bookId, startChapter = chapterIndex, startPositionMs = startPositionMs)
        }

        private suspend fun awaitConnection() {
            if (connected.value) return
            // Подождать подключения до ~5с; если future упал — попробовать переподключиться.
            val ok = withTimeoutOrNull(5_000L) { connected.first { it } }
            if (ok == null && !connected.value) {
                Log.w("PlayerViewModel", "controller not connected in 5s, reconnecting")
                connect()
                withTimeoutOrNull(5_000L) { connected.first { it } }
            }
        }

        fun pause() = viewModelScope.launch { playerController.pause() }

        fun resume() {
            if (connected.value) playerController.resume()
        }

        fun seek(positionMs: Long) = playerController.seekTo(positionMs)

        fun seekBack() = playerController.seekBack()

        fun seekForward() = playerController.seekForward()

        fun nextChapter() = playerController.nextChapter()

        fun previousChapter() = playerController.previousChapter()

        fun setSpeed(speed: Float) = playerController.setSpeed(speed)

        fun setVolumeBoost(b: Float) = playerController.setVolumeBoost(b)

        fun setEqualizer(preset: EqualizerPreset) = playerController.setEqualizer(preset)

        fun startSleep(ms: Long) = playerController.startSleep(ms)

        fun startSleepUntilChapterEnd(remainingMs: Long) = playerController.startSleepUntilChapterEnd(remainingMs)

        fun cancelSleep() = playerController.cancelSleep()

        fun addBookmark(title: String) = viewModelScope.launch { playerController.addBookmark(title) }

        override fun onCleared() {
            playerController.detach()
            super.onCleared()
        }
    }

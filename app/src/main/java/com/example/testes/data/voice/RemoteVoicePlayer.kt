package com.example.testes.data.voice

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import java.io.File

/**
 * Reprodutor exclusivo para WAVs vindos de /chat/speech.
 * Não sintetiza nada localmente — apenas reproduz bytes recebidos do backend.
 */
@OptIn(UnstableApi::class)
class RemoteVoicePlayer(context: Context) {

    private val cacheDir: File = context.cacheDir
    private val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private var currentFile: File? = null
    private var listener: Listener? = null

    interface Listener {
        fun onPlaybackStarted()
        fun onPlaybackEnded()
        fun onPlaybackError(message: String)
    }

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        Log.i(TAG, "ExoPlayer STATE_READY duration=${player.duration}ms")
                    }
                    Player.STATE_ENDED -> {
                        Log.i(TAG, "ExoPlayer STATE_ENDED")
                        clearFile()
                        listener?.onPlaybackEnded()
                    }
                    Player.STATE_IDLE -> Unit
                    Player.STATE_BUFFERING -> Log.i(TAG, "ExoPlayer STATE_BUFFERING")
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) listener?.onPlaybackStarted()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer error: ${error.errorCodeName} ${error.message}", error)
                clearFile()
                listener?.onPlaybackError(error.message ?: "Falha ao reproduzir o áudio.")
            }
        })
    }

    fun setListener(l: Listener?) {
        listener = l
    }

    fun play(bytes: ByteArray) {
        if (bytes.isEmpty()) {
            Log.w(TAG, "Empty payload — nothing to play")
            listener?.onPlaybackError("Resposta de áudio vazia.")
            return
        }
        stop()
        val file = File.createTempFile("renato-", ".wav", cacheDir).apply {
            writeBytes(bytes)
        }
        currentFile = file
        Log.i(TAG, "Saved WAV ${bytes.size} bytes → ${file.absolutePath}")
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
        player.prepare()
        player.playWhenReady = true
    }

    fun stop() {
        if (player.isPlaying || player.playbackState != Player.STATE_IDLE) {
            Log.i(TAG, "Stopping playback")
            player.stop()
        }
        clearFile()
    }

    fun release() {
        Log.i(TAG, "Releasing ExoPlayer")
        player.release()
        clearFile()
    }

    private fun clearFile() {
        currentFile?.let { runCatching { it.delete() } }
        currentFile = null
    }

    companion object {
        private const val TAG = "VoicePlayer"
    }
}

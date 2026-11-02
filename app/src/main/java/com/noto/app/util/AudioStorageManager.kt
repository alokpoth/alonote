package com.noto.app.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * 100% Offline Audio Voice Memo Manager.
 * Handles recording compressed AAC (.m4a) audio into internal private storage
 * and ultra-responsive low-latency playback with exact seek and progress reporting.
 */
object AudioStorageManager {
    private const val AUDIO_DIR = "note_audio"
    private const val TAG = "AudioStorageManager"

    fun getAudioDirectory(context: Context): File {
        val dir = File(context.filesDir, AUDIO_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getAudioFile(context: Context, filename: String): File {
        return File(getAudioDirectory(context), filename)
    }

    fun deleteAudio(context: Context, filename: String): Boolean {
        val file = getAudioFile(context, filename)
        return if (file.exists()) file.delete() else false
    }

    fun formatDuration(millis: Int): String {
        val totalSecs = (millis / 1000).coerceAtLeast(0)
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%d:%02d", mins, secs)
    }
}

/**
 * Clean wrapper for recording audio using MediaRecorder.
 */
class NoteAudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var recordingStartTime: Long = 0L

    var isRecording: Boolean = false
        private set

    fun startRecording(): String? {
        if (isRecording) return null

        val filename = "voice_${System.currentTimeMillis()}.m4a"
        val file = AudioStorageManager.getAudioFile(context, filename)
        file.parentFile?.mkdirs()
        currentFile = file

        return try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            filename
        } catch (e: Exception) {
            Log.e("NoteAudioRecorder", "Failed to start audio recording", e)
            currentFile?.delete()
            currentFile = null
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            null
        }
    }

    fun stopRecording(): String? {
        if (!isRecording) return null
        isRecording = false
        val filename = currentFile?.name

        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e("NoteAudioRecorder", "Error stopping recording", e)
        } finally {
            mediaRecorder = null
            currentFile = null
        }
        return filename
    }

    fun cancelRecording() {
        if (isRecording) {
            isRecording = false
            try {
                mediaRecorder?.apply {
                    stop()
                    reset()
                    release()
                }
            } catch (e: Exception) {
                Log.e("NoteAudioRecorder", "Error canceling recording", e)
            } finally {
                mediaRecorder = null
                currentFile?.delete()
                currentFile = null
            }
        }
    }
}

/**
 * Reactive Audio Player for playing in-note voice recordings.
 */
class NoteAudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _currentPlayingFile = MutableStateFlow<String?>(null)
    val currentPlayingFile: StateFlow<String?> = _currentPlayingFile.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    fun togglePlay(filename: String) {
        if (_currentPlayingFile.value == filename && _isPlaying.value) {
            pause()
        } else if (_currentPlayingFile.value == filename && !_isPlaying.value) {
            resume()
        } else {
            play(filename)
        }
    }

    fun play(filename: String) {
        val file = AudioStorageManager.getAudioFile(context, filename)
        if (!file.exists()) return

        stop()

        try {
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0
                    stopProgressJob()
                }
                start()
            }

            mediaPlayer = player
            _currentPlayingFile.value = filename
            _isPlaying.value = true
            _durationMs.value = player.duration
            _currentPositionMs.value = 0

            startProgressJob()
        } catch (e: Exception) {
            Log.e("NoteAudioPlayer", "Error playing audio $filename", e)
            stop()
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                stopProgressJob()
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _isPlaying.value = true
                startProgressJob()
            }
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.let {
            it.seekTo(positionMs)
            _currentPositionMs.value = positionMs
        }
    }

    fun stop() {
        stopProgressJob()
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (ignored: Exception) {}
            try {
                player.reset()
            } catch (ignored: Exception) {}
            try {
                player.release()
            } catch (ignored: Exception) {}
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentPlayingFile.value = null
        _currentPositionMs.value = 0
        _durationMs.value = 0
    }

    fun release() {
        stop()
        try {
            scope.coroutineContext.cancelChildren()
        } catch (ignored: Exception) {}
    }

    private fun startProgressJob() {
        stopProgressJob()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            _currentPositionMs.value = player.currentPosition
                        }
                    } catch (ignored: Exception) {}
                }
                delay(100)
            }
        }
    }

    private fun stopProgressJob() {
        progressJob?.cancel()
        progressJob = null
    }
}

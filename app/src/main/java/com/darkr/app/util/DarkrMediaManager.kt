package com.darkr.app.util

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent

/**
 * Standard Android Media Control Dispatcher.
 * Uses official Android AudioManager media key event dispatching to legitimately control
 * media playback (YouTube, YouTube Music, Spotify, Podcasts, Audiobooks) without scraping,
 * injection, or violating third-party app terms.
 */
class DarkrMediaManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun playPause() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    fun nextTrack() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun previousTrack() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    fun stop() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_STOP)
    }

    private fun sendMediaKeyEvent(keyCode: Int) {
        val am = audioManager ?: return
        val eventTime = SystemClock.uptimeMillis()

        try {
            val downEvent = KeyEvent(
                eventTime,
                eventTime,
                KeyEvent.ACTION_DOWN,
                keyCode,
                0
            )
            am.dispatchMediaKeyEvent(downEvent)

            val upEvent = KeyEvent(
                eventTime,
                eventTime + 50,
                KeyEvent.ACTION_UP,
                keyCode,
                0
            )
            am.dispatchMediaKeyEvent(upEvent)
        } catch (e: Exception) {
            // Safe fallback if key event is rejected by system
        }
    }
}

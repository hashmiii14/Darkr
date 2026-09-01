package com.darkr.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.darkr.app.databinding.ViewBlackoutBinding

/**
 * Pure AMOLED True Blackout overlay.
 * Double-tap anywhere to wake.
 */
class BlackoutView(
    context: Context,
    overlayManager: OverlayManager,
    private val onWakeListener: () -> Unit
) {

    private val binding: ViewBlackoutBinding =
        ViewBlackoutBinding.inflate(LayoutInflater.from(context))
    private val mainHandler = Handler(Looper.getMainLooper())
    private val fadeRunnable = Runnable {
        binding.layoutWakePrompt.animate().alpha(0f).setDuration(400).start()
    }

    val layoutParams: WindowManager.LayoutParams = overlayManager.createOverlayLayoutParams(
        width = WindowManager.LayoutParams.MATCH_PARENT,
        height = WindowManager.LayoutParams.MATCH_PARENT,
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        format = PixelFormat.OPAQUE
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    val rootView: View get() = binding.root

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            onWakeListener.invoke()
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            mainHandler.removeCallbacks(fadeRunnable)
            binding.layoutWakePrompt.animate().alpha(1f).setDuration(150).withEndAction {
                mainHandler.postDelayed(fadeRunnable, 1500)
            }.start()
            return true
        }
    })

    init {
        binding.root.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        mainHandler.postDelayed(fadeRunnable, 2000)
    }

    fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
    }
}

package com.darkr.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.darkr.app.databinding.ViewBlackoutBinding

class BlackoutView(
    context: Context,
    private val onWakeListener: () -> Unit
) {

    private val binding: ViewBlackoutBinding =
        ViewBlackoutBinding.inflate(LayoutInflater.from(context))

    val layoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        PixelFormat.OPAQUE
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        screenBrightness = 0.0f
    }

    val rootView: View get() = binding.root

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            onWakeListener.invoke()
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            // Flash subtle prompt on single tap
            binding.layoutWakePrompt.animate().alpha(1f).setDuration(150).withEndAction {
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.layoutWakePrompt.animate().alpha(0f).setDuration(400).start()
                }, 1500)
            }.start()
            return true
        }
    })

    init {
        binding.root.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        // Auto-fade helper prompt after 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            binding.layoutWakePrompt.animate().alpha(0f).setDuration(500).start()
        }, 2000)
    }
}

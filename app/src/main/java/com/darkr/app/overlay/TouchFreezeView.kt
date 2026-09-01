package com.darkr.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.darkr.app.databinding.ViewTouchFreezeBinding

/**
 * Touch Freeze Interceptor.
 * Consumes touch input while keeping the underlying display fully visible.
 */
class TouchFreezeView(
    context: Context,
    overlayManager: OverlayManager,
    private val onUnlockListener: () -> Unit
) {

    private val binding: ViewTouchFreezeBinding =
        ViewTouchFreezeBinding.inflate(LayoutInflater.from(context))

    val layoutParams: WindowManager.LayoutParams = overlayManager.createOverlayLayoutParams(
        width = WindowManager.LayoutParams.MATCH_PARENT,
        height = WindowManager.LayoutParams.MATCH_PARENT,
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        format = PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    val rootView: View get() = binding.root

    private val badgeGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            onUnlockListener.invoke()
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            pulseBadge()
            return true
        }
    })

    init {
        // Intercept all screen touches
        binding.root.setOnTouchListener { _, _ ->
            pulseBadge()
            true
        }

        // Double-tap on badge unlocks
        binding.badgeUnlock.setOnTouchListener { _, event ->
            badgeGestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun pulseBadge() {
        binding.badgeUnlock.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).withEndAction {
            binding.badgeUnlock.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()
    }
}

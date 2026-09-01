package com.darkr.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.darkr.app.databinding.ViewCamouflageBinding

/**
 * Emergency Camouflage Decoy screen (System Update screen).
 * Double-tap anywhere to dismiss decoy.
 */
class CamouflageView(
    context: Context,
    overlayManager: OverlayManager,
    private val onDismissListener: () -> Unit
) {

    private val binding: ViewCamouflageBinding =
        ViewCamouflageBinding.inflate(LayoutInflater.from(context))

    val layoutParams: WindowManager.LayoutParams = overlayManager.createOverlayLayoutParams(
        width = WindowManager.LayoutParams.MATCH_PARENT,
        height = WindowManager.LayoutParams.MATCH_PARENT,
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        format = PixelFormat.OPAQUE
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    val rootView: View get() = binding.root

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDismissListener.invoke()
            return true
        }
    })

    init {
        binding.root.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }
}

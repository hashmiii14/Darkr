package com.darkr.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.darkr.app.databinding.ViewTouchFreezeBinding

class TouchFreezeView(
    context: Context,
    private val onUnlockListener: () -> Unit
) {

    private val binding: ViewTouchFreezeBinding =
        ViewTouchFreezeBinding.inflate(LayoutInflater.from(context))

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
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    val rootView: View get() = binding.root

    private val badgeGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            onUnlockListener.invoke()
            return true
        }
    })

    init {
        // Intercept all touches on root (freeze touch)
        binding.root.setOnTouchListener { _, _ -> true }

        // Double tap on badge to unlock
        binding.badgeUnlock.setOnTouchListener { _, event ->
            badgeGestureDetector.onTouchEvent(event)
            true
        }
    }
}

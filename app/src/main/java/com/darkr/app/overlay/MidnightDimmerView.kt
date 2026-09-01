package com.darkr.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.darkr.app.databinding.ViewMidnightDimmerBinding

/**
 * Midnight Dimmer non-interactive luminance reduction overlay.
 */
class MidnightDimmerView(
    context: Context,
    overlayManager: OverlayManager
) {

    private val binding: ViewMidnightDimmerBinding =
        ViewMidnightDimmerBinding.inflate(LayoutInflater.from(context))

    val layoutParams: WindowManager.LayoutParams = overlayManager.createOverlayLayoutParams(
        width = WindowManager.LayoutParams.MATCH_PARENT,
        height = WindowManager.LayoutParams.MATCH_PARENT,
        flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        format = PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    val rootView: View get() = binding.root
}

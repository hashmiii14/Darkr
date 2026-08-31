package com.darkr.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.darkr.app.databinding.ViewPrivacyCurtainBinding
import com.darkr.app.util.PreferencesManager

class PrivacyCurtainView(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onCloseListener: () -> Unit
) {

    private val binding: ViewPrivacyCurtainBinding =
        ViewPrivacyCurtainBinding.inflate(LayoutInflater.from(context))
    private val prefs = PreferencesManager(context)

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

    private var initialTouchY = 0f
    private var initialMaskTopHeight = 0
    private var slitHeight = 140

    init {
        setupPositions()
        setupListeners()
    }

    private fun setupPositions() {
        val displayMetrics = context.resources.displayMetrics
        val density = displayMetrics.density
        slitHeight = (140 * density).toInt()

        val savedTopHeight = prefs.slitYPosition
        binding.maskTop.layoutParams.height = savedTopHeight
        binding.layoutSlit.layoutParams.height = slitHeight
        binding.root.requestLayout()
    }

    private fun setupListeners() {
        binding.btnCloseShield.setOnClickListener {
            onCloseListener.invoke()
        }

        // Dragging the Slit Up & Down
        binding.slitHeader.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchY = event.rawY
                    initialMaskTopHeight = binding.maskTop.height
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    val newTopHeight = (initialMaskTopHeight + deltaY).coerceAtLeast(50)
                    binding.maskTop.layoutParams.height = newTopHeight
                    binding.root.requestLayout()
                    prefs.slitYPosition = newTopHeight
                    true
                }
                else -> false
            }
        }
    }
}

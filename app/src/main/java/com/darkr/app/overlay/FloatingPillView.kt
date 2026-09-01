package com.darkr.app.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import com.darkr.app.databinding.ViewFloatingPillBinding
import com.darkr.app.util.PreferencesManager
import kotlin.math.abs

class FloatingPillView(
    private val context: Context,
    private val windowManager: WindowManager,
    private val listener: ActionListener
) {

    interface ActionListener {
        fun onShieldClicked()
        fun onBlackoutClicked()
        fun onFreezeClicked()
        fun onDimmerClicked()
        fun onCloseClicked()
    }

    private val binding: ViewFloatingPillBinding =
        ViewFloatingPillBinding.inflate(LayoutInflater.from(context))
    private val prefs = PreferencesManager(context)

    val layoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = prefs.pillX
        y = prefs.pillY
    }

    val rootView: View get() = binding.root

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var isExpanded = false

    init {
        setupTouchListener()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnQuickShield.setOnClickListener {
            toggleExpanded()
            listener.onShieldClicked()
        }
        binding.btnQuickBlackout.setOnClickListener {
            toggleExpanded()
            listener.onBlackoutClicked()
        }
        binding.btnQuickFreeze.setOnClickListener {
            toggleExpanded()
            listener.onFreezeClicked()
        }
        binding.btnQuickDim.setOnClickListener {
            toggleExpanded()
            listener.onDimmerClicked()
        }
        binding.btnQuickClose.setOnClickListener {
            toggleExpanded()
            listener.onCloseClicked()
        }
    }

    private fun setupTouchListener() {
        binding.pillContainer.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    if (abs(deltaX) > 10 || abs(deltaY) > 10) {
                        isDragging = true
                        layoutParams.x = initialX + deltaX
                        layoutParams.y = initialY + deltaY
                        try {
                            windowManager.updateViewLayout(binding.root, layoutParams)
                        } catch (e: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        toggleExpanded()
                    } else {
                        snapToNearestEdge()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleExpanded() {
        isExpanded = !isExpanded
        binding.layoutQuickActions.visibility = if (isExpanded) View.VISIBLE else View.GONE
    }

    private fun snapToNearestEdge() {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val targetX = if (layoutParams.x > screenWidth / 2) screenWidth - binding.root.width else 0

        val animator = ValueAnimator.ofInt(layoutParams.x, targetX).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                layoutParams.x = animation.animatedValue as Int
                try {
                    windowManager.updateViewLayout(binding.root, layoutParams)
                } catch (e: Exception) {}
            }
        }
        animator.start()

        prefs.pillX = targetX
        prefs.pillY = layoutParams.y
    }
}

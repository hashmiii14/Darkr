package com.darkr.app.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import com.darkr.app.databinding.ViewFloatingPillBinding
import com.darkr.app.util.PreferencesManager
import kotlin.math.abs

/**
 * 1-Tap Floating Ghost Toggle.
 * Features:
 * - Ultra-responsive 1-Tap Instant Blackout (No popups or menus).
 * - Semi-transparent idle state (alpha 0.5f) that wakes on touch (alpha 0.9f).
 * - Smooth physics-based dragging and edge snapping.
 */
@SuppressLint("ClickableViewAccessibility")
class FloatingPillView(
    private val context: Context,
    private val overlayManager: OverlayManager,
    private val onToggleBlackout: () -> Unit
) {

    private val binding: ViewFloatingPillBinding =
        ViewFloatingPillBinding.inflate(LayoutInflater.from(context))
    private val prefs = PreferencesManager(context)

    private val touchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop
    private val displayMetrics: DisplayMetrics = context.resources.displayMetrics
    private val density: Float = displayMetrics.density

    private val pillWidthPx: Int = (48 * density).toInt()
    private val pillHeightPx: Int = (48 * density).toInt()

    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialPillX = 0
    private var initialPillY = 0
    private var isDragging = false

    val layoutParams: WindowManager.LayoutParams = overlayManager.createOverlayLayoutParams(
        width = pillWidthPx,
        height = pillHeightPx,
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        format = PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = prefs.pillX
        y = prefs.pillY
    }

    val rootView: View get() = binding.root

    init {
        clampToScreenBounds()
        setupTouchInteraction()
        binding.root.alpha = 0.55f
    }

    private fun setupTouchInteraction() {
        binding.root.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    initialPillX = layoutParams.x
                    initialPillY = layoutParams.y
                    isDragging = false

                    binding.root.animate().scaleX(1.1f).scaleY(1.1f).alpha(0.95f).setDuration(120).start()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY

                    if (!isDragging && (abs(deltaX) > touchSlop || abs(deltaY) > touchSlop)) {
                        isDragging = true
                    }

                    if (isDragging) {
                        layoutParams.x = (initialPillX + deltaX).toInt()
                        layoutParams.y = (initialPillY + deltaY).toInt()
                        overlayManager.safeUpdateViewLayout(binding.root, layoutParams)
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    binding.root.animate().scaleX(1.0f).scaleY(1.0f).alpha(0.55f).setDuration(200).start()

                    if (isDragging) {
                        snapToNearestEdge()
                    } else {
                        // 1-TAP INSTANT BLACKOUT TRIGGER!
                        vibrateHaptic()
                        onToggleBlackout.invoke()
                    }
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    binding.root.animate().scaleX(1.0f).scaleY(1.0f).alpha(0.55f).setDuration(200).start()
                    if (isDragging) {
                        snapToNearestEdge()
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun snapToNearestEdge() {
        val screenWidth = displayMetrics.widthPixels
        val currentX = layoutParams.x
        val targetX = if (currentX + pillWidthPx / 2 < screenWidth / 2) 0 else screenWidth - pillWidthPx

        val startX = layoutParams.x
        val animator = ValueAnimator.ofInt(startX, targetX).apply {
            duration = 220
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                layoutParams.x = animation.animatedValue as Int
                overlayManager.safeUpdateViewLayout(binding.root, layoutParams)
            }
        }
        animator.start()

        prefs.pillX = targetX
        prefs.pillY = layoutParams.y
    }

    private fun clampToScreenBounds() {
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        layoutParams.x = layoutParams.x.coerceIn(0, (screenWidth - pillWidthPx).coerceAtLeast(0))
        layoutParams.y = layoutParams.y.coerceIn(100, (screenHeight - pillHeightPx - 100).coerceAtLeast(100))
    }

    private fun vibrateHaptic() {
        if (!prefs.isVibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(20)
            }
        } catch (e: Exception) {
            // Ignore vibration error
        }
    }

    fun onDestroy() {
        // Cleanup
    }
}

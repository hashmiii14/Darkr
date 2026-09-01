package com.darkr.app.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
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
 * Premium monochrome Floating Action Orb and Quick Action HUD.
 */
class FloatingPillView(
    private val context: Context,
    private val overlayManager: OverlayManager,
    private val listener: ActionListener
) {

    interface ActionListener {
        fun onShieldClicked()
        fun onBlackoutClicked()
        fun onFreezeClicked()
        fun onDimmerClicked()
        fun onPanicClicked()
        fun onCloseClicked()
    }

    private val binding: ViewFloatingPillBinding =
        ViewFloatingPillBinding.inflate(LayoutInflater.from(context))
    private val prefs = PreferencesManager(context)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    val layoutParams: WindowManager.LayoutParams = overlayManager.createOverlayLayoutParams(
        width = WindowManager.LayoutParams.WRAP_CONTENT,
        height = WindowManager.LayoutParams.WRAP_CONTENT,
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        format = PixelFormat.TRANSLUCENT
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
    private var snapAnimator: ValueAnimator? = null

    init {
        setupClickListeners()
        setupTouchListener()
    }

    private fun setupClickListeners() {
        binding.btnQuickShield.setOnClickListener {
            collapseHud()
            listener.onShieldClicked()
        }
        binding.btnQuickBlackout.setOnClickListener {
            collapseHud()
            listener.onBlackoutClicked()
        }
        binding.btnQuickFreeze.setOnClickListener {
            collapseHud()
            listener.onFreezeClicked()
        }
        binding.btnQuickDim.setOnClickListener {
            collapseHud()
            listener.onDimmerClicked()
        }
        binding.btnQuickPanic.setOnClickListener {
            collapseHud()
            listener.onPanicClicked()
        }
    }

    private fun setupTouchListener() {
        binding.pillContainer.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    snapAnimator?.cancel()
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

                    if (!isDragging && (abs(deltaX) > touchSlop || abs(deltaY) > touchSlop)) {
                        isDragging = true
                        if (isExpanded) collapseHud()
                    }

                    if (isDragging) {
                        layoutParams.x = initialX + deltaX
                        layoutParams.y = clampY(initialY + deltaY)
                        overlayManager.safeUpdateViewLayout(binding.root, layoutParams)
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

    private fun clampY(y: Int): Int {
        val displayMetrics = context.resources.displayMetrics
        val minY = 60
        val maxY = displayMetrics.heightPixels - 200
        return y.coerceIn(minY, maxY)
    }

    private fun toggleExpanded() {
        if (isExpanded) {
            collapseHud()
        } else {
            expandHud()
        }
    }

    fun expandHud() {
        isExpanded = true
        binding.layoutQuickActions.visibility = View.VISIBLE
        binding.layoutQuickActions.alpha = 0f
        binding.layoutQuickActions.animate().alpha(1f).setDuration(150).start()
    }

    fun collapseHud() {
        isExpanded = false
        binding.layoutQuickActions.animate().alpha(0f).setDuration(120).withEndAction {
            binding.layoutQuickActions.visibility = View.GONE
        }.start()
    }

    private fun snapToNearestEdge() {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val pillWidth = binding.pillContainer.width.takeIf { it > 0 } ?: (52 * displayMetrics.density).toInt()
        val targetX = if (layoutParams.x + (pillWidth / 2) > screenWidth / 2) {
            screenWidth - pillWidth - 16
        } else {
            16
        }

        snapAnimator?.cancel()
        snapAnimator = ValueAnimator.ofInt(layoutParams.x, targetX).apply {
            duration = 220
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                layoutParams.x = anim.animatedValue as Int
                overlayManager.safeUpdateViewLayout(binding.root, layoutParams)
            }
        }
        snapAnimator?.start()

        prefs.pillX = targetX
        prefs.pillY = layoutParams.y
    }

    fun onDestroy() {
        snapAnimator?.cancel()
        snapAnimator = null
    }
}

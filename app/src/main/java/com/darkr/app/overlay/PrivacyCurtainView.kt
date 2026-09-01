package com.darkr.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.darkr.app.databinding.ViewCurtainBottomBinding
import com.darkr.app.databinding.ViewCurtainTopBinding
import com.darkr.app.util.PreferencesManager

/**
 * Dual-segment Privacy Curtain overlay.
 * Uses split top/bottom window masks to ensure 100% native touch passthrough in the viewing gap.
 */
class PrivacyCurtainView(
    private val context: Context,
    private val overlayManager: OverlayManager,
    private val onCloseListener: () -> Unit
) {

    private val bindingTop = ViewCurtainTopBinding.inflate(LayoutInflater.from(context))
    private val bindingBottom = ViewCurtainBottomBinding.inflate(LayoutInflater.from(context))
    private val prefs = PreferencesManager(context)

    private val displayMetrics = context.resources.displayMetrics
    private val screenHeight = displayMetrics.heightPixels
    private val handleHeight = (32 * displayMetrics.density).toInt()
    private val slitGapHeight = (150 * displayMetrics.density).toInt()

    private var currentTopHeight = prefs.slitYPosition.coerceIn(80, screenHeight - slitGapHeight - 200)

    val topLayoutParams: WindowManager.LayoutParams = overlayManager.createOverlayLayoutParams(
        width = WindowManager.LayoutParams.MATCH_PARENT,
        height = currentTopHeight + handleHeight,
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        format = PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 0
        y = 0
    }

    val bottomLayoutParams: WindowManager.LayoutParams = overlayManager.createOverlayLayoutParams(
        width = WindowManager.LayoutParams.MATCH_PARENT,
        height = (screenHeight - (currentTopHeight + handleHeight + slitGapHeight)).coerceAtLeast(100),
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        format = PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 0
        y = currentTopHeight + handleHeight + slitGapHeight
    }

    val topView: View get() = bindingTop.root
    val bottomView: View get() = bindingBottom.root

    private var initialTouchY = 0f
    private var initialTopHeight = 0

    init {
        setupListeners()
        applyMaskHeights(currentTopHeight)
    }

    private fun setupListeners() {
        bindingTop.btnCloseCurtain.setOnClickListener {
            onCloseListener.invoke()
        }

        bindingTop.slitDragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchY = event.rawY
                    initialTopHeight = currentTopHeight
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    val maxTop = screenHeight - slitGapHeight - handleHeight - 120
                    val newTopHeight = (initialTopHeight + deltaY).coerceIn(60, maxTop)
                    if (newTopHeight != currentTopHeight) {
                        currentTopHeight = newTopHeight
                        applyMaskHeights(currentTopHeight)
                        prefs.slitYPosition = currentTopHeight
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun applyMaskHeights(topHeight: Int) {
        bindingTop.maskTopBody.layoutParams.height = topHeight
        bindingTop.maskTopBody.requestLayout()

        topLayoutParams.height = topHeight + handleHeight
        overlayManager.safeUpdateViewLayout(bindingTop.root, topLayoutParams)

        val bottomY = topHeight + handleHeight + slitGapHeight
        val bottomH = (screenHeight - bottomY).coerceAtLeast(60)

        bottomLayoutParams.y = bottomY
        bottomLayoutParams.height = bottomH
        overlayManager.safeUpdateViewLayout(bindingBottom.root, bottomLayoutParams)
    }
}

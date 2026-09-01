package com.darkr.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager

/**
 * Safe, hardened WindowManager overlay manager.
 * Guarantees idempotency, prevents duplicate addView/removeView leaks and catches window token failures gracefully.
 */
class OverlayManager(private val context: Context) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val activeViews = mutableSetOf<View>()

    val isWindowAttached: Boolean get() = activeViews.isNotEmpty()

    fun createOverlayLayoutParams(
        width: Int = WindowManager.LayoutParams.MATCH_PARENT,
        height: Int = WindowManager.LayoutParams.MATCH_PARENT,
        flags: Int = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        format: Int = PixelFormat.TRANSLUCENT
    ): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(width, height, type, flags, format).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    @Synchronized
    fun safeAddView(view: View, params: WindowManager.LayoutParams): Boolean {
        if (activeViews.contains(view) || view.isAttachedToWindow) {
            Log.w(TAG, "safeAddView: View $view is already attached to WindowManager")
            return true
        }
        return try {
            windowManager.addView(view, params)
            activeViews.add(view)
            Log.d(TAG, "safeAddView: Successfully added $view")
            true
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "safeAddView: BadTokenException adding overlay view", e)
            false
        } catch (e: IllegalStateException) {
            Log.e(TAG, "safeAddView: IllegalStateException adding overlay view", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "safeAddView: Unexpected error adding overlay view", e)
            false
        }
    }

    @Synchronized
    fun safeUpdateViewLayout(view: View, params: WindowManager.LayoutParams): Boolean {
        if (!activeViews.contains(view) && !view.isAttachedToWindow) {
            Log.w(TAG, "safeUpdateViewLayout: View $view is not attached")
            return false
        }
        return try {
            windowManager.updateViewLayout(view, params)
            true
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "safeUpdateViewLayout: View not found to update", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "safeUpdateViewLayout: Error updating overlay view", e)
            false
        }
    }

    @Synchronized
    fun safeRemoveView(view: View?): Boolean {
        if (view == null) return true
        if (!activeViews.contains(view) && !view.isAttachedToWindow) {
            activeViews.remove(view)
            return true
        }
        return try {
            windowManager.removeView(view)
            activeViews.remove(view)
            Log.d(TAG, "safeRemoveView: Successfully removed $view")
            true
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "safeRemoveView: View was not attached to window manager", e)
            activeViews.remove(view)
            false
        } catch (e: Exception) {
            Log.e(TAG, "safeRemoveView: Error removing overlay view", e)
            activeViews.remove(view)
            false
        }
    }

    @Synchronized
    fun removeAll() {
        val viewsToRemove = activeViews.toList()
        for (view in viewsToRemove) {
            safeRemoveView(view)
        }
        activeViews.clear()
    }

    companion object {
        private const val TAG = "OverlayManager"
    }
}

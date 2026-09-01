package com.darkr.app.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import com.darkr.app.databinding.ViewBlackoutBinding
import com.darkr.app.util.PreferencesManager
import com.darkr.app.util.StatsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 100% Zero-Leak Pure AMOLED Blackout Engine.
 * Features:
 * - Complete screen blackout covering status bar, navigation bar, and display cutouts.
 * - Minimalist centered World Clock, Day/Date/Month, and live Battery percentage.
 * - Tap-to-Reveal bottom unlock pill button with auto-fade timeout.
 * - Double-tap anywhere failsafe wake gesture.
 * - Programmatic pixel-shifting to prevent OLED burn-in.
 * - Accurate session duration tracking with StatsManager.
 */
class BlackoutView(
    private val context: Context,
    overlayManager: OverlayManager,
    private val onWakeListener: () -> Unit
) {

    private val binding: ViewBlackoutBinding =
        ViewBlackoutBinding.inflate(LayoutInflater.from(context))
    private val prefs = PreferencesManager(context)
    private val statsManager = StatsManager(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var pixelShiftIndex = 0
    private var isUnlockPillVisible = false

    val layoutParams: WindowManager.LayoutParams = overlayManager.createOverlayLayoutParams(
        width = WindowManager.LayoutParams.MATCH_PARENT,
        height = WindowManager.LayoutParams.MATCH_PARENT,
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        format = PixelFormat.OPAQUE
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    val rootView: View get() = binding.root

    private val fadeUnlockPillRunnable = Runnable {
        binding.layoutUnlockPill.animate()
            .alpha(0f)
            .setDuration(400)
            .withEndAction {
                isUnlockPillVisible = false
            }
            .start()
    }

    private val clockUpdateRunnable = object : Runnable {
        override fun run() {
            updateClockDisplay()
            applyBurnInPixelShift()
            mainHandler.postDelayed(this, 30_000) // Update every 30s
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                if (level >= 0 && scale > 0) {
                    val batteryPct = (level * 100) / scale
                    val statusText = if (isCharging) "CHARGING ⚡" else "AMOLED SAVING"
                    binding.tvClockBattery.text = "🔋 $batteryPct% • $statusText"
                }
            }
        }
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            performUnlock()
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            revealUnlockPill()
            return true
        }
    })

    init {
        statsManager.startBlackoutSession()
        setupImmersiveZeroLeak()
        setupGestures()
        setupClockDisplay()
        setupUnlockButton()
    }

    private fun setupImmersiveZeroLeak() {
        binding.root.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowInsetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.systemBars())
                    controller.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                or View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        )
            }
        }
    }

    private fun setupGestures() {
        binding.root.setOnTouchListener { _, event ->
            // Check if touch hits the unlock pill directly
            if (isUnlockPillVisible && event.action == MotionEvent.ACTION_UP) {
                val location = IntArray(2)
                binding.layoutUnlockPill.getLocationOnScreen(location)
                val x = event.rawX
                val y = event.rawY
                if (x >= location[0] && x <= location[0] + binding.layoutUnlockPill.width &&
                    y >= location[1] && y <= location[1] + binding.layoutUnlockPill.height) {
                    performUnlock()
                    return@setOnTouchListener true
                }
            }

            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun setupClockDisplay() {
        if (prefs.isClockEnabled) {
            binding.layoutClockContainer.visibility = View.VISIBLE
            binding.tvClockDate.visibility = if (prefs.isShowDate) View.VISIBLE else View.GONE
            binding.tvClockBattery.visibility = if (prefs.isShowBattery) View.VISIBLE else View.GONE

            updateClockDisplay()
            mainHandler.post(clockUpdateRunnable)

            try {
                val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                context.registerReceiver(batteryReceiver, filter)
            } catch (e: Exception) {
                // Ignore receiver registration error
            }
        } else {
            binding.layoutClockContainer.visibility = View.GONE
        }
    }

    private fun setupUnlockButton() {
        binding.layoutUnlockPill.setOnClickListener {
            performUnlock()
        }
    }

    /**
     * Reveals the bottom unlock pill smoothly when user touches the screen.
     */
    private fun revealUnlockPill() {
        mainHandler.removeCallbacks(fadeUnlockPillRunnable)
        isUnlockPillVisible = true
        binding.layoutUnlockPill.animate()
            .alpha(1f)
            .setDuration(180)
            .withEndAction {
                mainHandler.postDelayed(fadeUnlockPillRunnable, 3500)
            }
            .start()
    }

    private fun performUnlock() {
        vibrateHaptic()
        statsManager.endBlackoutSession()
        onWakeListener.invoke()
    }

    private fun updateClockDisplay() {
        val now = Date()
        val pattern = if (prefs.isTime24Hour) "HH:mm" else "hh:mm a"
        val timeFormat = SimpleDateFormat(pattern, Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())

        binding.tvClockTime.text = timeFormat.format(now)
        binding.tvClockDate.text = dateFormat.format(now)
    }

    /**
     * Shifts clock X/Y coordinates slightly to prevent OLED burn-in.
     */
    private fun applyBurnInPixelShift() {
        pixelShiftIndex = (pixelShiftIndex + 1) % 4
        val shiftAmountDp = 4
        val density = context.resources.displayMetrics.density
        val shiftPx = (shiftAmountDp * density).toInt()

        val (shiftX, shiftY) = when (pixelShiftIndex) {
            0 -> Pair(shiftPx, shiftPx)
            1 -> Pair(-shiftPx, shiftPx)
            2 -> Pair(-shiftPx, -shiftPx)
            else -> Pair(shiftPx, -shiftPx)
        }

        binding.layoutClockContainer.translationX = shiftX.toFloat()
        binding.layoutClockContainer.translationY = shiftY.toFloat()
    }

    private fun vibrateHaptic() {
        if (!prefs.isVibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(30)
            }
        } catch (e: Exception) {
            // Ignore vibration failure
        }
    }

    fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        statsManager.endBlackoutSession()
        try {
            if (prefs.isClockEnabled) {
                context.unregisterReceiver(batteryReceiver)
            }
        } catch (e: Exception) {
            // Ignore unregister failure
        }
    }
}

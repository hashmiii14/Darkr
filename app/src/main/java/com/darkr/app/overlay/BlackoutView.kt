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
import android.util.DisplayMetrics
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
 * Covers 100% of the display including status bar, notch, and bottom 3-button navigation bar.
 * Matches Black Screen by japp.io layout (Centered Time, Date, Battery, and bottom UNLOCK button).
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

    // Measure exact physical display dimensions
    private val realMetrics: DisplayMetrics = DisplayMetrics().apply {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(this)
    }

    val layoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
        realMetrics.widthPixels,
        realMetrics.heightPixels + 300, // Extend past navigation bar to guarantee 100% coverage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        PixelFormat.OPAQUE
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 0
        y = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
    }

    val rootView: View get() = binding.root

    private val fadeUnlockRunnable = Runnable {
        binding.tvUnlockText.animate()
            .alpha(0.2f)
            .setDuration(400)
            .start()
    }

    private val clockUpdateRunnable = object : Runnable {
        override fun run() {
            updateClockDisplay()
            applyBurnInPixelShift()
            mainHandler.postDelayed(this, 30_000)
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
                    val statusText = if (isCharging) "CHARGING" else "SAVING"
                    binding.tvClockBattery.text = "$batteryPct% • $statusText"
                }
            }
        }
    }

    init {
        statsManager.startBlackoutSession()
        setupImmersiveZeroLeak()
        setupClockDisplay()
        setupUnlockButton()

        binding.tvUnlockText.alpha = 0.2f
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
        // Tapping UNLOCK button dismisses the blackout
        binding.layoutUnlockContainer.setOnClickListener {
            performUnlock()
        }

        // Tapping anywhere on the screen highlights the UNLOCK button
        binding.root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val location = IntArray(2)
                binding.layoutUnlockContainer.getLocationOnScreen(location)
                val x = event.rawX
                val y = event.rawY

                if (x >= location[0] && x <= location[0] + binding.layoutUnlockContainer.width &&
                    y >= location[1] && y <= location[1] + binding.layoutUnlockContainer.height) {
                    performUnlock()
                    return@setOnTouchListener true
                }

                // Highlight unlock button smoothly
                mainHandler.removeCallbacks(fadeUnlockRunnable)
                binding.tvUnlockText.animate()
                    .alpha(1f)
                    .setDuration(150)
                    .withEndAction {
                        mainHandler.postDelayed(fadeUnlockRunnable, 3500)
                    }
                    .start()
            }
            true
        }
    }

    private fun performUnlock() {
        vibrateHaptic()
        statsManager.endBlackoutSession()
        onWakeListener.invoke()
    }

    private fun updateClockDisplay() {
        val now = Date()
        val is24H = prefs.isTime24Hour

        when (prefs.clockStyle) {
            PreferencesManager.STYLE_MODERN_MINIMAL -> {
                binding.tvClockTime.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
                binding.tvClockTime.textSize = 64f
                val pattern = if (is24H) "HH:mm" else "h:mm"
                binding.tvClockTime.text = SimpleDateFormat(pattern, Locale.getDefault()).format(now)
                binding.tvClockDate.text = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(now)
            }
            PreferencesManager.STYLE_BOLD_MONO -> {
                binding.tvClockTime.typeface = android.graphics.Typeface.MONOSPACE
                binding.tvClockTime.textSize = 58f
                val pattern = if (is24H) "HH:mm" else "hh:mm"
                binding.tvClockTime.text = SimpleDateFormat(pattern, Locale.getDefault()).format(now)
                binding.tvClockDate.text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(now)
            }
            PreferencesManager.STYLE_ELEGANCE_THIN -> {
                binding.tvClockTime.typeface = android.graphics.Typeface.create("sans-serif-thin", android.graphics.Typeface.NORMAL)
                binding.tvClockTime.textSize = 72f
                val pattern = if (is24H) "HH:mm" else "hh:mm a"
                binding.tvClockTime.text = SimpleDateFormat(pattern, Locale.getDefault()).format(now)
                binding.tvClockDate.text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)
            }
            PreferencesManager.STYLE_OLED_MATRIX -> {
                binding.tvClockTime.typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
                binding.tvClockTime.textSize = 64f
                val pattern = if (is24H) "HH:mm" else "h:mm"
                binding.tvClockTime.text = SimpleDateFormat(pattern, Locale.getDefault()).format(now)
                binding.tvClockDate.text = SimpleDateFormat("d MMM", Locale.getDefault()).format(now).uppercase()
            }
            else -> {
                binding.tvClockTime.typeface = android.graphics.Typeface.DEFAULT_BOLD
                binding.tvClockTime.textSize = 64f
                val pattern = if (is24H) "HH:mm" else "h:mm"
                binding.tvClockTime.text = SimpleDateFormat(pattern, Locale.getDefault()).format(now)
                binding.tvClockDate.text = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(now)
            }
        }
    }

    /**
     * Shifts clock X/Y coordinates slightly to prevent OLED burn-in.
     */
    private fun applyBurnInPixelShift() {
        pixelShiftIndex = (pixelShiftIndex + 1) % 4
        val shiftAmountDp = 3
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
                    VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(25)
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

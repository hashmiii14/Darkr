package com.darkr.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.darkr.app.databinding.ViewBlackoutBinding
import com.darkr.app.util.DarkrStateManager
import com.darkr.app.util.PreferencesManager
import com.darkr.app.util.StatsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 100% Zero-Leak Edge-to-Edge Blackout Activity.
 * Guaranteed 100% full-screen pitch-black coverage over status bar and 3-button navigation bar.
 * Features:
 * - Always visible, glowing UNLOCK button
 * - Double-tap anywhere failsafe wake
 * - Live battery and customizable clock styles
 * - Burn-in pixel shifting protection
 */
class BlackoutActivity : AppCompatActivity() {

    private lateinit var binding: ViewBlackoutBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var statsManager: StatsManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var pixelShiftIndex = 0

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

    private val gestureDetector by lazy {
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                performUnlock()
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                binding.tvUnlockText.animate()
                    .alpha(1.0f)
                    .scaleX(1.08f)
                    .scaleY(1.08f)
                    .setDuration(120)
                    .withEndAction {
                        binding.tvUnlockText.animate()
                            .alpha(0.8f)
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(200)
                            .start()
                    }
                    .start()
                return true
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureZeroLeakWindow()

        binding = ViewBlackoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)
        statsManager = StatsManager(this)

        DarkrStateManager.setBlackoutActive(true)
        statsManager.startBlackoutSession()

        setupClockDisplay()
        setupUnlockInteraction()
    }

    private fun configureZeroLeakWindow() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun setupClockDisplay() {
        if (prefs.isClockEnabled) {
            binding.layoutClockContainer.visibility = View.VISIBLE
            binding.tvClockDate.visibility = if (prefs.isShowDate) View.VISIBLE else View.GONE
            binding.tvClockBattery.visibility = if (prefs.isShowBattery) View.VISIBLE else View.GONE

            updateClockDisplay()
            mainHandler.post(clockUpdateRunnable)

            try {
                registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            } catch (e: Exception) {
                // Ignore receiver error
            }
        } else {
            binding.layoutClockContainer.visibility = View.GONE
        }
    }

    private fun setupUnlockInteraction() {
        binding.layoutUnlockContainer.setOnClickListener {
            performUnlock()
        }

        binding.root.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
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
        }
    }

    private fun applyBurnInPixelShift() {
        pixelShiftIndex = (pixelShiftIndex + 1) % 4
        val shiftAmountDp = 3
        val density = resources.displayMetrics.density
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

    private fun performUnlock() {
        vibrateHaptic()
        statsManager.endBlackoutSession()
        DarkrStateManager.setBlackoutActive(false)
        finish()
        overridePendingTransition(0, android.R.anim.fade_out)
    }

    private fun vibrateHaptic() {
        if (!prefs.isVibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(25)
            }
        } catch (e: Exception) {
            // Ignore vibration error
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
        statsManager.endBlackoutSession()
        DarkrStateManager.setBlackoutActive(false)
        try {
            if (prefs.isClockEnabled) {
                unregisterReceiver(batteryReceiver)
            }
        } catch (e: Exception) {
            // Ignore error
        }
    }
}

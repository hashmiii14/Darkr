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
import com.darkr.app.util.DarkrMediaManager
import com.darkr.app.util.PreferencesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * Pure AMOLED Zero-Leak Blackout overlay.
 * Features:
 * - Total zero-leak display blackout covering notches, cutouts, status and nav bars.
 * - Optional minimal OLED clock with automatic pixel-shift burn-in protection.
 * - Non-intrusive media playback controls (Play/Pause, Next, Prev).
 * - Failsafe double-tap wake with haptic feedback.
 */
class BlackoutView(
    private val context: Context,
    overlayManager: OverlayManager,
    private val onWakeListener: () -> Unit
) {

    private val binding: ViewBlackoutBinding =
        ViewBlackoutBinding.inflate(LayoutInflater.from(context))
    private val prefs = PreferencesManager(context)
    private val mediaManager = DarkrMediaManager(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isPlaying = true
    private var pixelShiftIndex = 0

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

    private val fadeRunnable = Runnable {
        binding.layoutWakePrompt.animate().alpha(0f).setDuration(400).start()
    }

    private val clockUpdateRunnable = object : Runnable {
        override fun run() {
            updateClockDisplay()
            applyBurnInPixelShift()
            mainHandler.postDelayed(this, 60_000) // Update every minute
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    val batteryPct = (level * 100) / scale
                    binding.tvClockBattery.text = "$batteryPct% • OLED SAVING"
                }
            }
        }
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            vibrateHaptic()
            onWakeListener.invoke()
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            // Briefly reveal wake helper prompt
            mainHandler.removeCallbacks(fadeRunnable)
            binding.layoutWakePrompt.animate().alpha(1f).setDuration(150).withEndAction {
                mainHandler.postDelayed(fadeRunnable, 1500)
            }.start()
            return true
        }
    })

    init {
        setupImmersiveFlags()
        setupGestures()
        setupClockMode()
        setupMediaControls()

        mainHandler.postDelayed(fadeRunnable, 2000)
    }

    private fun setupImmersiveFlags() {
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
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun setupClockMode() {
        if (prefs.isClockEnabled) {
            binding.layoutClockContainer.visibility = View.VISIBLE
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

    private fun setupMediaControls() {
        if (prefs.isMediaControlsEnabled) {
            binding.layoutMediaBar.visibility = View.VISIBLE

            binding.btnMediaPlayPause.setOnClickListener {
                vibrateHaptic()
                mediaManager.playPause()
                isPlaying = !isPlaying
                binding.btnMediaPlayPause.setImageResource(
                    if (isPlaying) com.darkr.app.R.drawable.ic_media_pause else com.darkr.app.R.drawable.ic_media_play
                )
            }

            binding.btnMediaNext.setOnClickListener {
                vibrateHaptic()
                mediaManager.nextTrack()
            }

            binding.btnMediaPrev.setOnClickListener {
                vibrateHaptic()
                mediaManager.previousTrack()
            }
        } else {
            binding.layoutMediaBar.visibility = View.GONE
        }
    }

    private fun updateClockDisplay() {
        val now = Date()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
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
        try {
            if (prefs.isClockEnabled) {
                context.unregisterReceiver(batteryReceiver)
            }
        } catch (e: Exception) {
            // Ignore unregister failure
        }
    }
}

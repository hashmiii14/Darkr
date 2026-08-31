package com.darkr.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.darkr.app.DarkrApplication
import com.darkr.app.MainActivity
import com.darkr.app.R
import com.darkr.app.overlay.*
import com.darkr.app.sensor.ShakeDetector
import com.darkr.app.util.PreferencesManager

class DarkrOverlayService : Service(), FloatingPillView.ActionListener {

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: PreferencesManager

    private var floatingPill: FloatingPillView? = null
    private var privacyCurtain: PrivacyCurtainView? = null
    private var blackoutView: BlackoutView? = null
    private var touchFreezeView: TouchFreezeView? = null
    private var midnightDimmerView: MidnightDimmerView? = null
    private var camouflageView: CamouflageView? = null
    private var shakeDetector: ShakeDetector? = null

    companion object {
        const val ACTION_START = "com.darkr.app.ACTION_START"
        const val ACTION_STOP = "com.darkr.app.ACTION_STOP"
        const val ACTION_TOGGLE_SHIELD = "com.darkr.app.ACTION_TOGGLE_SHIELD"
        const val ACTION_TOGGLE_BLACKOUT = "com.darkr.app.ACTION_TOGGLE_BLACKOUT"
        const val ACTION_TOGGLE_FREEZE = "com.darkr.app.ACTION_TOGGLE_FREEZE"
        const val ACTION_TOGGLE_DIMMER = "com.darkr.app.ACTION_TOGGLE_DIMMER"
        const val NOTIFICATION_ID = 1001

        var isRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = PreferencesManager(this)
        isRunning = true

        startForeground(NOTIFICATION_ID, buildNotification())
        setupFloatingPill()
        setupShakeDetector()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_SHIELD -> onShieldClicked()
            ACTION_TOGGLE_BLACKOUT -> onBlackoutClicked()
            ACTION_TOGGLE_FREEZE -> onFreezeClicked()
            ACTION_TOGGLE_DIMMER -> onDimmerClicked()
        }
        return START_STICKY
    }

    private fun setupFloatingPill() {
        if (floatingPill == null) {
            floatingPill = FloatingPillView(this, windowManager, this)
            try {
                windowManager.addView(floatingPill?.rootView, floatingPill?.layoutParams)
            } catch (_: Exception) {}
        }
    }

    private fun setupShakeDetector() {
        if (prefs.isPanicEnabled) {
            shakeDetector = ShakeDetector(this) {
                // On Panic Shake -> Trigger Blackout or Camouflage
                onPanicTriggered()
            }
            shakeDetector?.start()
        }
    }

    private fun onPanicTriggered() {
        if (blackoutView == null && camouflageView == null) {
            onBlackoutClicked()
        }
    }

    override fun onShieldClicked() {
        if (privacyCurtain == null) {
            privacyCurtain = PrivacyCurtainView(this, windowManager) {
                removePrivacyCurtain()
            }
            try {
                windowManager.addView(privacyCurtain?.rootView, privacyCurtain?.layoutParams)
            } catch (_: Exception) {}
        } else {
            removePrivacyCurtain()
        }
    }

    private fun removePrivacyCurtain() {
        privacyCurtain?.let {
            try {
                windowManager.removeView(it.rootView)
            } catch (_: Exception) {}
            privacyCurtain = null
        }
    }

    override fun onBlackoutClicked() {
        if (blackoutView == null) {
            blackoutView = BlackoutView(this) {
                removeBlackout()
            }
            try {
                windowManager.addView(blackoutView?.rootView, blackoutView?.layoutParams)
            } catch (_: Exception) {}
        } else {
            removeBlackout()
        }
    }

    private fun removeBlackout() {
        blackoutView?.let {
            try {
                windowManager.removeView(it.rootView)
            } catch (_: Exception) {}
            blackoutView = null
        }
    }

    override fun onFreezeClicked() {
        if (touchFreezeView == null) {
            touchFreezeView = TouchFreezeView(this) {
                removeTouchFreeze()
            }
            try {
                windowManager.addView(touchFreezeView?.rootView, touchFreezeView?.layoutParams)
            } catch (_: Exception) {}
        } else {
            removeTouchFreeze()
        }
    }

    private fun removeTouchFreeze() {
        touchFreezeView?.let {
            try {
                windowManager.removeView(it.rootView)
            } catch (_: Exception) {}
            touchFreezeView = null
        }
    }

    override fun onDimmerClicked() {
        if (midnightDimmerView == null) {
            midnightDimmerView = MidnightDimmerView(this)
            try {
                windowManager.addView(midnightDimmerView?.rootView, midnightDimmerView?.layoutParams)
            } catch (_: Exception) {}
        } else {
            removeDimmer()
        }
    }

    private fun removeDimmer() {
        midnightDimmerView?.let {
            try {
                windowManager.removeView(it.rootView)
            } catch (_: Exception) {}
            midnightDimmerView = null
        }
    }

    override fun onCloseClicked() {
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, DarkrApplication.CHANNEL_ID)
            .setContentTitle("Darkr Shield Active")
            .setContentText("Tap floating pill to trigger Privacy Shield or AMOLED Blackout")
            .setSmallIcon(R.drawable.ic_darkr_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        prefs.isServiceEnabled = false

        shakeDetector?.stop()
        removePrivacyCurtain()
        removeBlackout()
        removeTouchFreeze()
        removeDimmer()

        floatingPill?.let {
            try {
                windowManager.removeView(it.rootView)
            } catch (_: Exception) {}
            floatingPill = null
        }
    }
}

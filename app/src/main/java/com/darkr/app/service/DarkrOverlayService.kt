package com.darkr.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.darkr.app.DarkrApplication
import com.darkr.app.MainActivity
import com.darkr.app.R
import com.darkr.app.overlay.*
import com.darkr.app.sensor.ShakeDetector
import com.darkr.app.util.DarkrStateManager
import com.darkr.app.util.PreferencesManager

/**
 * Foreground Service controlling the Darkr Floating Orb, Privacy Curtains,
 * Blackout, Touch Freeze, Midnight Dimmer, and Shake sensor.
 */
class DarkrOverlayService : Service(), FloatingPillView.ActionListener {

    private lateinit var overlayManager: OverlayManager
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
        const val ACTION_TOGGLE_CAMOUFLAGE = "com.darkr.app.ACTION_TOGGLE_CAMOUFLAGE"
        const val ACTION_TRIGGER_PANIC = "com.darkr.app.ACTION_TRIGGER_PANIC"
        const val NOTIFICATION_ID = 1001

        private const val TAG = "DarkrOverlayService"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        overlayManager = OverlayManager(this)
        prefs = PreferencesManager(this)

        DarkrStateManager.setServiceRunning(true)
        DarkrStateManager.setShakeEnabled(prefs.isPanicEnabled)
        DarkrStateManager.setPanicMode(prefs.panicMode)

        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
        }

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
            ACTION_TOGGLE_CAMOUFLAGE -> onCamouflageClicked()
            ACTION_TRIGGER_PANIC -> onPanicClicked()
            ACTION_START -> {
                // If service already running, ensure floating pill is attached
                if (floatingPill == null) {
                    setupFloatingPill()
                }
            }
        }
        return START_STICKY
    }

    private fun setupFloatingPill() {
        if (floatingPill == null) {
            floatingPill = FloatingPillView(this, overlayManager, this)
            floatingPill?.let {
                overlayManager.safeAddView(it.rootView, it.layoutParams)
            }
        }
    }

    private fun setupShakeDetector() {
        if (prefs.isPanicEnabled) {
            if (shakeDetector == null) {
                shakeDetector = ShakeDetector(this) {
                    onPanicClicked()
                }
            }
            shakeDetector?.start()
        } else {
            shakeDetector?.stop()
            shakeDetector = null
        }
    }

    override fun onShieldClicked() {
        if (privacyCurtain == null) {
            privacyCurtain = PrivacyCurtainView(this, overlayManager) {
                removePrivacyCurtain()
            }
            privacyCurtain?.let {
                overlayManager.safeAddView(it.topView, it.topLayoutParams)
                overlayManager.safeAddView(it.bottomView, it.bottomLayoutParams)
                DarkrStateManager.setShieldActive(true)
            }
        } else {
            removePrivacyCurtain()
        }
    }

    private fun removePrivacyCurtain() {
        privacyCurtain?.let {
            overlayManager.safeRemoveView(it.topView)
            overlayManager.safeRemoveView(it.bottomView)
            privacyCurtain = null
            DarkrStateManager.setShieldActive(false)
        }
    }

    override fun onBlackoutClicked() {
        if (blackoutView == null) {
            // Dismiss camouflage if active
            removeCamouflage()

            blackoutView = BlackoutView(this, overlayManager) {
                removeBlackout()
            }
            blackoutView?.let {
                overlayManager.safeAddView(it.rootView, it.layoutParams)
                DarkrStateManager.setBlackoutActive(true)
            }
        } else {
            removeBlackout()
        }
    }

    private fun removeBlackout() {
        blackoutView?.let {
            it.onDestroy()
            overlayManager.safeRemoveView(it.rootView)
            blackoutView = null
            DarkrStateManager.setBlackoutActive(false)
        }
    }

    override fun onFreezeClicked() {
        if (touchFreezeView == null) {
            touchFreezeView = TouchFreezeView(this, overlayManager) {
                removeTouchFreeze()
            }
            touchFreezeView?.let {
                overlayManager.safeAddView(it.rootView, it.layoutParams)
                DarkrStateManager.setFreezeActive(true)
            }
        } else {
            removeTouchFreeze()
        }
    }

    private fun removeTouchFreeze() {
        touchFreezeView?.let {
            overlayManager.safeRemoveView(it.rootView)
            touchFreezeView = null
            DarkrStateManager.setFreezeActive(false)
        }
    }

    override fun onDimmerClicked() {
        if (midnightDimmerView == null) {
            midnightDimmerView = MidnightDimmerView(this, overlayManager)
            midnightDimmerView?.let {
                overlayManager.safeAddView(it.rootView, it.layoutParams)
                DarkrStateManager.setDimmerActive(true)
            }
        } else {
            removeDimmer()
        }
    }

    private fun removeDimmer() {
        midnightDimmerView?.let {
            overlayManager.safeRemoveView(it.rootView)
            midnightDimmerView = null
            DarkrStateManager.setDimmerActive(false)
        }
    }

    fun onCamouflageClicked() {
        if (camouflageView == null) {
            removeBlackout()

            camouflageView = CamouflageView(this, overlayManager) {
                removeCamouflage()
            }
            camouflageView?.let {
                overlayManager.safeAddView(it.rootView, it.layoutParams)
                DarkrStateManager.setCamouflageActive(true)
            }
        } else {
            removeCamouflage()
        }
    }

    private fun removeCamouflage() {
        camouflageView?.let {
            overlayManager.safeRemoveView(it.rootView)
            camouflageView = null
            DarkrStateManager.setCamouflageActive(false)
        }
    }

    override fun onPanicClicked() {
        when (prefs.panicMode) {
            PreferencesManager.PANIC_MODE_CAMOUFLAGE -> onCamouflageClicked()
            else -> onBlackoutClicked()
        }
    }

    override fun onCloseClicked() {
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, DarkrApplication.CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(R.drawable.ic_darkr_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.isServiceEnabled = false
        DarkrStateManager.setServiceRunning(false)

        shakeDetector?.stop()
        shakeDetector = null

        removePrivacyCurtain()
        removeBlackout()
        removeTouchFreeze()
        removeDimmer()
        removeCamouflage()

        floatingPill?.onDestroy()
        floatingPill?.let {
            overlayManager.safeRemoveView(it.rootView)
            floatingPill = null
        }

        overlayManager.removeAll()
    }
}

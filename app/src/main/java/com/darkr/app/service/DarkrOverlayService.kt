package com.darkr.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.core.app.NotificationCompat
import com.darkr.app.BlackoutActivity
import com.darkr.app.DarkrApplication
import com.darkr.app.MainActivity
import com.darkr.app.R
import com.darkr.app.overlay.*
import com.darkr.app.sensor.PocketDetector
import com.darkr.app.util.DarkrStateManager
import com.darkr.app.util.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Core Foreground Service controlling the Darkr Floating Ghost Toggle,
 * Zero-Leak Blackout Engine, and Smart Pocket Sensor.
 * Automatically hides the floating toggle when blackout is active.
 */
class DarkrOverlayService : Service() {

    private lateinit var overlayManager: OverlayManager
    private lateinit var prefs: PreferencesManager
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var floatingPill: FloatingPillView? = null
    private var privacyCurtain: PrivacyCurtainView? = null
    private var blackoutView: BlackoutView? = null
    private var pocketDetector: PocketDetector? = null

    companion object {
        const val ACTION_START = "com.darkr.app.ACTION_START"
        const val ACTION_STOP = "com.darkr.app.ACTION_STOP"
        const val ACTION_TOGGLE_SHIELD = "com.darkr.app.ACTION_TOGGLE_SHIELD"
        const val ACTION_TOGGLE_BLACKOUT = "com.darkr.app.ACTION_TOGGLE_BLACKOUT"
        const val ACTION_REFRESH_SENSORS = "com.darkr.app.ACTION_REFRESH_SENSORS"
        const val NOTIFICATION_ID = 1001

        private const val TAG = "DarkrOverlayService"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        overlayManager = OverlayManager(this)
        prefs = PreferencesManager(this)

        DarkrStateManager.setServiceRunning(true)
        DarkrStateManager.setPocketEnabled(prefs.isPocketEnabled)

        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
        }

        setupFloatingPill()
        setupSensors()
        observeBlackoutState()
    }

    private fun observeBlackoutState() {
        serviceScope.launch {
            DarkrStateManager.isBlackoutActive.collect { isBlackout ->
                floatingPill?.rootView?.visibility = if (isBlackout) View.GONE else View.VISIBLE
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_SHIELD -> onShieldClicked()
            ACTION_TOGGLE_BLACKOUT -> onBlackoutClicked()
            ACTION_REFRESH_SENSORS -> setupSensors()
            ACTION_START -> {
                if (floatingPill == null) {
                    setupFloatingPill()
                }
                setupSensors()
            }
        }
        return START_STICKY
    }

    private fun setupFloatingPill() {
        if (floatingPill == null) {
            floatingPill = FloatingPillView(this, overlayManager) {
                onBlackoutClicked()
            }
            floatingPill?.let {
                overlayManager.safeAddView(it.rootView, it.layoutParams)
            }
        }
    }

    private fun setupSensors() {
        if (prefs.isPocketEnabled) {
            if (pocketDetector == null) {
                pocketDetector = PocketDetector(this) { isInPocket ->
                    if (isInPocket) {
                        if (!DarkrStateManager.isBlackoutActive.value) {
                            onBlackoutClicked()
                        }
                    } else {
                        if (DarkrStateManager.isBlackoutActive.value) {
                            removeBlackout()
                        }
                    }
                }
            }
            pocketDetector?.startListening()
        } else {
            pocketDetector?.stopListening()
            pocketDetector = null
        }
    }

    fun onShieldClicked() {
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

    fun onBlackoutClicked() {
        floatingPill?.rootView?.visibility = View.GONE
        try {
            val intent = Intent(this, BlackoutActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            if (blackoutView == null) {
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
    }

    private fun removeBlackout() {
        blackoutView?.let {
            it.onDestroy()
            overlayManager.safeRemoveView(it.rootView)
            blackoutView = null
            DarkrStateManager.setBlackoutActive(false)
        }
        floatingPill?.rootView?.visibility = View.VISIBLE
    }

    private fun buildNotification(): Notification {
        val appPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleBlackoutIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, DarkrOverlayService::class.java).apply {
                action = ACTION_TOGGLE_BLACKOUT
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, DarkrOverlayService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, DarkrApplication.CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(R.drawable.ic_darkr_logo)
            .setContentIntent(appPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(R.drawable.ic_blackout, "Blackout", toggleBlackoutIntent)
            .addAction(R.drawable.ic_darkr_logo, "Stop", stopIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.isServiceEnabled = false
        DarkrStateManager.setServiceRunning(false)

        pocketDetector?.stopListening()
        pocketDetector = null

        removePrivacyCurtain()
        removeBlackout()

        floatingPill?.onDestroy()
        floatingPill?.let {
            overlayManager.safeRemoveView(it.rootView)
            floatingPill = null
        }

        overlayManager.removeAll()
    }
}

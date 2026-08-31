package com.darkr.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class DarkrApplication : Application() {

    companion object {
        const val CHANNEL_ID = "darkr_foreground_service"
        const val CHANNEL_NAME = "Darkr Protection Active"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent status when Darkr screen overlay is active"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}

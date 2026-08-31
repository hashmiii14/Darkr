package com.darkr.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.darkr.app.service.DarkrOverlayService
import com.darkr.app.util.PreferencesManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = PreferencesManager(context)
            if (prefs.isServiceEnabled && Settings.canDrawOverlays(context)) {
                val serviceIntent = Intent(context, DarkrOverlayService::class.java).apply {
                    action = DarkrOverlayService.ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}

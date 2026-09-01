package com.darkr.app.service

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.darkr.app.MainActivity
import com.darkr.app.R
import com.darkr.app.util.DarkrStateManager

/**
 * Native Android Quick Settings Tile Service.
 * Allows users to toggle instantaneous display blackout directly from the system Quick Settings
 * shade on any active app (e.g. YouTube, YouTube Music, Spotify) without opening Darkr dashboard.
 */
@RequiresApi(Build.VERSION_CODES.N)
class DarkrTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        // Verify overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }

        // Toggle Blackout
        val isServiceRunning = DarkrStateManager.isServiceRunning.value
        if (!isServiceRunning) {
            val startIntent = Intent(this, DarkrOverlayService::class.java).apply {
                action = DarkrOverlayService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, startIntent)
            } else {
                startService(startIntent)
            }
        }

        val toggleIntent = Intent(this, DarkrOverlayService::class.java).apply {
            action = DarkrOverlayService.ACTION_TOGGLE_BLACKOUT
        }
        startService(toggleIntent)

        // Optimistically flip tile state
        val tile = qsTile ?: return
        tile.state = if (tile.state == Tile.STATE_ACTIVE) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        tile.updateTile()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isBlackoutActive = DarkrStateManager.isBlackoutActive.value

        tile.state = if (isBlackoutActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.app_name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isBlackoutActive) "Blackout Active" else "Tap to Blackout"
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_blackout)
        tile.updateTile()
    }
}

package com.darkr.app.util

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AMOLED Battery & Blackout Usage Tracker.
 * Accurately tracks blackout session duration, total blacked-out screen time,
 * session count, and estimates battery mAh & percentage saved on OLED displays.
 */
class StatsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var currentSessionStartTime: Long = 0

    fun startBlackoutSession() {
        currentSessionStartTime = System.currentTimeMillis()
    }

    fun endBlackoutSession() {
        if (currentSessionStartTime == 0L) return

        val now = System.currentTimeMillis()
        val durationSeconds = ((now - currentSessionStartTime) / 1000).coerceAtLeast(1)
        currentSessionStartTime = 0L

        // Record total seconds
        val totalSeconds = getTotalBlackoutSeconds() + durationSeconds
        prefs.edit().putLong(KEY_TOTAL_BLACKOUT_SECONDS, totalSeconds).apply()

        // Record today's seconds
        val todayKey = getTodayKey()
        val todaySeconds = prefs.getLong(todayKey, 0L) + durationSeconds
        prefs.edit().putLong(todayKey, todaySeconds).apply()

        // Increment session count
        val totalSessions = getTotalSessions() + 1
        prefs.edit().putInt(KEY_TOTAL_SESSIONS, totalSessions).apply()
    }

    fun getTodayBlackoutSeconds(): Long {
        return prefs.getLong(getTodayKey(), 0L)
    }

    fun getTotalBlackoutSeconds(): Long {
        return prefs.getLong(KEY_TOTAL_BLACKOUT_SECONDS, 0L)
    }

    fun getTotalSessions(): Int {
        return prefs.getInt(KEY_TOTAL_SESSIONS, 0)
    }

    /**
     * Estimated battery saved in mAh (based on standard OLED display power draw:
     * ~200-300 mW/hr reduction when switching full display pixels to #000000 black).
     * Approx ~50 mAh saved per 10 minutes of blackout on standard 4500-5000mAh phones (~300 mAh/hr).
     */
    fun getEstimatedBatterySavedMah(): Int {
        val totalSeconds = getTotalBlackoutSeconds()
        val hours = totalSeconds / 3600.0
        return (hours * 260.0).toInt() // ~260 mAh per hour of blackout
    }

    /**
     * Estimated battery percentage saved on typical 4500mAh battery.
     */
    fun getEstimatedBatterySavedPercent(): Int {
        val savedMah = getEstimatedBatterySavedMah()
        return ((savedMah / 4500.0) * 100).toInt().coerceAtMost(100)
    }

    fun formatDuration(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60

        return when {
            hrs > 0 -> "${hrs}h ${mins}m"
            mins > 0 -> "${mins}m ${secs}s"
            else -> "${secs}s"
        }
    }

    private fun getTodayKey(): String {
        val sdf = SimpleDateFormat("yyyy_MM_dd", Locale.US)
        return KEY_TODAY_PREFIX + sdf.format(Date())
    }

    companion object {
        private const val PREFS_NAME = "darkr_stats_prefs"
        private const val KEY_TOTAL_BLACKOUT_SECONDS = "total_blackout_seconds"
        private const val KEY_TODAY_PREFIX = "today_seconds_"
        private const val KEY_TOTAL_SESSIONS = "total_sessions"
    }
}

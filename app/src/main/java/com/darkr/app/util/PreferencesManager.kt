package com.darkr.app.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Thread-safe preferences storage layer for Darkr persistent configuration.
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_ENABLED, value).apply()

    var isClockEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLOCK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_CLOCK_ENABLED, value).apply()

    var isTime24Hour: Boolean
        get() = prefs.getBoolean(KEY_TIME_24H, false)
        set(value) = prefs.edit().putBoolean(KEY_TIME_24H, value).apply()

    var isShowDate: Boolean
        get() = prefs.getBoolean(KEY_SHOW_DATE, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_DATE, value).apply()

    var isShowBattery: Boolean
        get() = prefs.getBoolean(KEY_SHOW_BATTERY, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_BATTERY, value).apply()

    var isPocketEnabled: Boolean
        get() = prefs.getBoolean(KEY_POCKET_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_POCKET_ENABLED, value).apply()

    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()

    var clockStyle: Int
        get() = prefs.getInt(KEY_CLOCK_STYLE, STYLE_MODERN_MINIMAL)
        set(value) = prefs.edit().putInt(KEY_CLOCK_STYLE, value).apply()

    var slitYPosition: Int
        get() = prefs.getInt(KEY_SLIT_Y, 400).coerceAtLeast(0)
        set(value) = prefs.edit().putInt(KEY_SLIT_Y, value.coerceAtLeast(0)).apply()

    var pillX: Int
        get() = prefs.getInt(KEY_PILL_X, 0)
        set(value) = prefs.edit().putInt(KEY_PILL_X, value).apply()

    var pillY: Int
        get() = prefs.getInt(KEY_PILL_Y, 500)
        set(value) = prefs.edit().putInt(KEY_PILL_Y, value).apply()

    companion object {
        const val PREFS_NAME = "darkr_settings"
        const val KEY_SERVICE_ENABLED = "key_service_enabled"
        const val KEY_CLOCK_ENABLED = "key_clock_enabled"
        const val KEY_TIME_24H = "key_time_24h"
        const val KEY_SHOW_DATE = "key_show_date"
        const val KEY_SHOW_BATTERY = "key_show_battery"
        const val KEY_POCKET_ENABLED = "key_pocket_enabled"
        const val KEY_VIBRATION_ENABLED = "key_vibration_enabled"
        const val KEY_CLOCK_STYLE = "key_clock_style"
        const val KEY_SLIT_Y = "key_slit_y"
        const val KEY_PILL_X = "key_pill_x"
        const val KEY_PILL_Y = "key_pill_y"

        const val STYLE_MODERN_MINIMAL = 0
        const val STYLE_BOLD_MONO = 1
        const val STYLE_ELEGANCE_THIN = 2
        const val STYLE_OLED_MATRIX = 3

        const val PANIC_MODE_BLACKOUT = "blackout"
    }
}

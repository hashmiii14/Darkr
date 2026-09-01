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

    var isShieldEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHIELD_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SHIELD_ENABLED, value).apply()

    var isBlackoutEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLACKOUT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BLACKOUT_ENABLED, value).apply()

    var isFreezeEnabled: Boolean
        get() = prefs.getBoolean(KEY_FREEZE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_FREEZE_ENABLED, value).apply()

    var isPanicEnabled: Boolean
        get() = prefs.getBoolean(KEY_PANIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_PANIC_ENABLED, value).apply()

    var isDimmerEnabled: Boolean
        get() = prefs.getBoolean(KEY_DIMMER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DIMMER_ENABLED, value).apply()

    var isClockEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLOCK_ENABLED, true) // Default enabled on blackout
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

    var isMediaControlsEnabled: Boolean
        get() = prefs.getBoolean(KEY_MEDIA_CONTROLS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_MEDIA_CONTROLS_ENABLED, value).apply()

    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()

    var panicMode: String
        get() = prefs.getString(KEY_PANIC_MODE, PANIC_MODE_BLACKOUT) ?: PANIC_MODE_BLACKOUT
        set(value) = prefs.edit().putString(KEY_PANIC_MODE, value).apply()

    var slitYPosition: Int
        get() = prefs.getInt(KEY_SLIT_Y, 400).coerceAtLeast(0)
        set(value) = prefs.edit().putInt(KEY_SLIT_Y, value.coerceAtLeast(0)).apply()

    var slitHeight: Int
        get() = prefs.getInt(KEY_SLIT_HEIGHT, 160).coerceIn(80, 800)
        set(value) = prefs.edit().putInt(KEY_SLIT_HEIGHT, value.coerceIn(80, 800)).apply()

    var pillX: Int
        get() = prefs.getInt(KEY_PILL_X, 0)
        set(value) = prefs.edit().putInt(KEY_PILL_X, value).apply()

    var pillY: Int
        get() = prefs.getInt(KEY_PILL_Y, 500)
        set(value) = prefs.edit().putInt(KEY_PILL_Y, value).apply()

    var floatingOrbSize: Int
        get() = prefs.getInt(KEY_FLOATING_ORB_SIZE, 1) // 0=Small, 1=Medium, 2=Large
        set(value) = prefs.edit().putInt(KEY_FLOATING_ORB_SIZE, value).apply()

    var floatingOrbOpacity: Float
        get() = prefs.getFloat(KEY_FLOATING_ORB_OPACITY, 0.6f)
        set(value) = prefs.edit().putFloat(KEY_FLOATING_ORB_OPACITY, value).apply()

    companion object {
        const val PREFS_NAME = "darkr_settings"
        const val KEY_SERVICE_ENABLED = "key_service_enabled"
        const val KEY_SHIELD_ENABLED = "key_shield_enabled"
        const val KEY_BLACKOUT_ENABLED = "key_blackout_enabled"
        const val KEY_FREEZE_ENABLED = "key_freeze_enabled"
        const val KEY_PANIC_ENABLED = "key_panic_enabled"
        const val KEY_DIMMER_ENABLED = "key_dimmer_enabled"
        const val KEY_CLOCK_ENABLED = "key_clock_enabled"
        const val KEY_TIME_24H = "key_time_24h"
        const val KEY_SHOW_DATE = "key_show_date"
        const val KEY_SHOW_BATTERY = "key_show_battery"
        const val KEY_POCKET_ENABLED = "key_pocket_enabled"
        const val KEY_MEDIA_CONTROLS_ENABLED = "key_media_controls_enabled"
        const val KEY_VIBRATION_ENABLED = "key_vibration_enabled"
        const val KEY_PANIC_MODE = "key_panic_mode"
        const val KEY_SLIT_Y = "key_slit_y"
        const val KEY_SLIT_HEIGHT = "key_slit_height"
        const val KEY_PILL_X = "key_pill_x"
        const val KEY_PILL_Y = "key_pill_y"
        const val KEY_FLOATING_ORB_SIZE = "key_floating_orb_size"
        const val KEY_FLOATING_ORB_OPACITY = "key_floating_orb_opacity"

        const val PANIC_MODE_BLACKOUT = "blackout"
        const val PANIC_MODE_CAMOUFLAGE = "camouflage"
    }
}

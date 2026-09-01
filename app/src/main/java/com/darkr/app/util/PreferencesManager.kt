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

    companion object {
        const val PREFS_NAME = "darkr_settings"
        const val KEY_SERVICE_ENABLED = "key_service_enabled"
        const val KEY_SHIELD_ENABLED = "key_shield_enabled"
        const val KEY_BLACKOUT_ENABLED = "key_blackout_enabled"
        const val KEY_FREEZE_ENABLED = "key_freeze_enabled"
        const val KEY_PANIC_ENABLED = "key_panic_enabled"
        const val KEY_DIMMER_ENABLED = "key_dimmer_enabled"
        const val KEY_PANIC_MODE = "key_panic_mode"
        const val KEY_SLIT_Y = "key_slit_y"
        const val KEY_SLIT_HEIGHT = "key_slit_height"
        const val KEY_PILL_X = "key_pill_x"
        const val KEY_PILL_Y = "key_pill_y"

        const val PANIC_MODE_BLACKOUT = "blackout"
        const val PANIC_MODE_CAMOUFLAGE = "camouflage"
    }
}

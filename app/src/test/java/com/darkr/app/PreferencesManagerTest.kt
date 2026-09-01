package com.darkr.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.darkr.app.util.PreferencesManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PreferencesManagerTest {

    private lateinit var context: Context
    private lateinit var prefs: PreferencesManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val sharedPrefs = context.getSharedPreferences(PreferencesManager.PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().commit()
        prefs = PreferencesManager(context)
    }

    @Test
    fun testDefaultValues() {
        assertFalse("Default service state should be false", prefs.isServiceEnabled)
        assertTrue("Default clock state should be true", prefs.isClockEnabled)
        assertFalse("Default 24-hour state should be false", prefs.isTime24Hour)
        assertTrue("Default show date state should be true", prefs.isShowDate)
        assertTrue("Default show battery state should be true", prefs.isShowBattery)
        assertFalse("Default pocket state should be false", prefs.isPocketEnabled)
        assertTrue("Default vibration state should be true", prefs.isVibrationEnabled)
        assertEquals("Default clock style should be Modern Minimal", PreferencesManager.STYLE_MODERN_MINIMAL, prefs.clockStyle)
    }

    @Test
    fun testPreferenceMutationAndPersistence() {
        prefs.isServiceEnabled = true
        assertTrue(prefs.isServiceEnabled)

        prefs.clockStyle = PreferencesManager.STYLE_BOLD_MONO
        assertEquals(PreferencesManager.STYLE_BOLD_MONO, prefs.clockStyle)

        prefs.isTime24Hour = true
        assertTrue(prefs.isTime24Hour)

        prefs.pillX = 120
        prefs.pillY = 340
        assertEquals(120, prefs.pillX)
        assertEquals(340, prefs.pillY)
    }
}

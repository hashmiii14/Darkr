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
        assertTrue("Default shield state should be true", prefs.isShieldEnabled)
        assertTrue("Default blackout state should be true", prefs.isBlackoutEnabled)
        assertTrue("Default freeze state should be true", prefs.isFreezeEnabled)
        assertTrue("Default panic state should be true", prefs.isPanicEnabled)
        assertFalse("Default dimmer state should be false", prefs.isDimmerEnabled)
        assertEquals("Default panic mode should be blackout", PreferencesManager.PANIC_MODE_BLACKOUT, prefs.panicMode)
        assertEquals("Default slit height should be 160", 160, prefs.slitHeight)
    }

    @Test
    fun testPreferenceMutationAndPersistence() {
        prefs.isServiceEnabled = true
        assertTrue(prefs.isServiceEnabled)

        prefs.isShieldEnabled = false
        assertFalse(prefs.isShieldEnabled)

        prefs.panicMode = PreferencesManager.PANIC_MODE_CAMOUFLAGE
        assertEquals(PreferencesManager.PANIC_MODE_CAMOUFLAGE, prefs.panicMode)

        prefs.pillX = 120
        prefs.pillY = 340
        assertEquals(120, prefs.pillX)
        assertEquals(340, prefs.pillY)
    }

    @Test
    fun testSlitHeightClamping() {
        prefs.slitHeight = 10
        assertEquals("Slit height should clamp to minimum 80", 80, prefs.slitHeight)

        prefs.slitHeight = 1200
        assertEquals("Slit height should clamp to maximum 800", 800, prefs.slitHeight)
    }
}

package com.darkr.app

import com.darkr.app.util.DarkrStateManager
import com.darkr.app.util.PreferencesManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DarkrStateManagerTest {

    @Before
    fun setup() {
        DarkrStateManager.resetAll()
    }

    @Test
    fun testInitialState() {
        assertFalse(DarkrStateManager.isServiceRunning.value)
        assertFalse(DarkrStateManager.isShieldActive.value)
        assertFalse(DarkrStateManager.isBlackoutActive.value)
        assertFalse(DarkrStateManager.isClockActive.value)
    }

    @Test
    fun testServiceStopResetsAllOverlays() {
        DarkrStateManager.setServiceRunning(true)
        DarkrStateManager.setShieldActive(true)
        DarkrStateManager.setBlackoutActive(true)
        DarkrStateManager.setClockActive(true)

        assertTrue(DarkrStateManager.isShieldActive.value)
        assertTrue(DarkrStateManager.isBlackoutActive.value)
        assertTrue(DarkrStateManager.isClockActive.value)

        // Stopping service must reset all overlay states
        DarkrStateManager.setServiceRunning(false)

        assertFalse(DarkrStateManager.isServiceRunning.value)
        assertFalse(DarkrStateManager.isShieldActive.value)
        assertFalse(DarkrStateManager.isBlackoutActive.value)
        assertFalse(DarkrStateManager.isClockActive.value)
    }

    @Test
    fun testPocketStateUpdates() {
        DarkrStateManager.setPocketEnabled(true)
        assertTrue(DarkrStateManager.isPocketEnabled.value)

        DarkrStateManager.setPocketEnabled(false)
        assertFalse(DarkrStateManager.isPocketEnabled.value)
    }
}

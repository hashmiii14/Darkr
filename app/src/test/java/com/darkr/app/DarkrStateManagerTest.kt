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
        assertFalse(DarkrStateManager.isFreezeActive.value)
        assertFalse(DarkrStateManager.isDimmerActive.value)
        assertFalse(DarkrStateManager.isCamouflageActive.value)
    }

    @Test
    fun testServiceStopResetsAllOverlays() {
        DarkrStateManager.setServiceRunning(true)
        DarkrStateManager.setShieldActive(true)
        DarkrStateManager.setBlackoutActive(true)
        DarkrStateManager.setFreezeActive(true)
        DarkrStateManager.setDimmerActive(true)

        assertTrue(DarkrStateManager.isShieldActive.value)
        assertTrue(DarkrStateManager.isBlackoutActive.value)

        // Stopping service must reset all overlay states
        DarkrStateManager.setServiceRunning(false)

        assertFalse(DarkrStateManager.isServiceRunning.value)
        assertFalse(DarkrStateManager.isShieldActive.value)
        assertFalse(DarkrStateManager.isBlackoutActive.value)
        assertFalse(DarkrStateManager.isFreezeActive.value)
        assertFalse(DarkrStateManager.isDimmerActive.value)
    }

    @Test
    fun testBlackoutAndCamouflageMutualExclusivity() {
        DarkrStateManager.setCamouflageActive(true)
        assertTrue(DarkrStateManager.isCamouflageActive.value)
        assertFalse(DarkrStateManager.isBlackoutActive.value)

        DarkrStateManager.setBlackoutActive(true)
        assertTrue(DarkrStateManager.isBlackoutActive.value)
        assertFalse(DarkrStateManager.isCamouflageActive.value)
    }

    @Test
    fun testPanicModeUpdate() {
        DarkrStateManager.setPanicMode(PreferencesManager.PANIC_MODE_CAMOUFLAGE)
        assertEquals(PreferencesManager.PANIC_MODE_CAMOUFLAGE, DarkrStateManager.panicMode.value)
    }
}

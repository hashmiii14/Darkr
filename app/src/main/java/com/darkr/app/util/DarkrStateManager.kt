package com.darkr.app.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central reactive single-source-of-truth state manager for Darkr.
 * Synchronizes runtime state across MainActivity, DarkrOverlayService, and Overlays.
 */
object DarkrStateManager {

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _isShieldActive = MutableStateFlow(false)
    val isShieldActive: StateFlow<Boolean> = _isShieldActive.asStateFlow()

    private val _isBlackoutActive = MutableStateFlow(false)
    val isBlackoutActive: StateFlow<Boolean> = _isBlackoutActive.asStateFlow()

    private val _isFreezeActive = MutableStateFlow(false)
    val isFreezeActive: StateFlow<Boolean> = _isFreezeActive.asStateFlow()

    private val _isDimmerActive = MutableStateFlow(false)
    val isDimmerActive: StateFlow<Boolean> = _isDimmerActive.asStateFlow()

    private val _isClockActive = MutableStateFlow(false)
    val isClockActive: StateFlow<Boolean> = _isClockActive.asStateFlow()

    private val _isCamouflageActive = MutableStateFlow(false)
    val isCamouflageActive: StateFlow<Boolean> = _isCamouflageActive.asStateFlow()

    private val _isShakeEnabled = MutableStateFlow(true)
    val isShakeEnabled: StateFlow<Boolean> = _isShakeEnabled.asStateFlow()

    private val _isPocketEnabled = MutableStateFlow(false)
    val isPocketEnabled: StateFlow<Boolean> = _isPocketEnabled.asStateFlow()

    private val _isMediaControlsVisible = MutableStateFlow(true)
    val isMediaControlsVisible: StateFlow<Boolean> = _isMediaControlsVisible.asStateFlow()

    private val _panicMode = MutableStateFlow(PreferencesManager.PANIC_MODE_BLACKOUT)
    val panicMode: StateFlow<String> = _panicMode.asStateFlow()

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
        if (!running) {
            // Reset active overlay states when service terminates
            _isShieldActive.value = false
            _isBlackoutActive.value = false
            _isFreezeActive.value = false
            _isDimmerActive.value = false
            _isClockActive.value = false
            _isCamouflageActive.value = false
        }
    }

    fun setShieldActive(active: Boolean) {
        _isShieldActive.value = active
    }

    fun setBlackoutActive(active: Boolean) {
        _isBlackoutActive.value = active
        if (active) {
            _isCamouflageActive.value = false
        }
    }

    fun setFreezeActive(active: Boolean) {
        _isFreezeActive.value = active
    }

    fun setDimmerActive(active: Boolean) {
        _isDimmerActive.value = active
    }

    fun setClockActive(active: Boolean) {
        _isClockActive.value = active
    }

    fun setCamouflageActive(active: Boolean) {
        _isCamouflageActive.value = active
        if (active) {
            _isBlackoutActive.value = false
        }
    }

    fun setShakeEnabled(enabled: Boolean) {
        _isShakeEnabled.value = enabled
    }

    fun setPocketEnabled(enabled: Boolean) {
        _isPocketEnabled.value = enabled
    }

    fun setMediaControlsVisible(visible: Boolean) {
        _isMediaControlsVisible.value = visible
    }

    fun setPanicMode(mode: String) {
        _panicMode.value = mode
    }

    fun resetAll() {
        _isServiceRunning.value = false
        _isShieldActive.value = false
        _isBlackoutActive.value = false
        _isFreezeActive.value = false
        _isDimmerActive.value = false
        _isClockActive.value = false
        _isCamouflageActive.value = false
    }
}

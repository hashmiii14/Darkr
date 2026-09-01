package com.darkr.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.darkr.app.databinding.ActivityMainBinding
import com.darkr.app.service.DarkrOverlayService
import com.darkr.app.util.DarkrStateManager
import com.darkr.app.util.PreferencesManager
import kotlinx.coroutines.launch

/**
 * Darkr Primary Dashboard Activity.
 * Synchronizes real-time state with DarkrStateManager and DarkrOverlayService.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager

    private var isUpdatingSwitchesProgrammatically = false

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissions()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Notification permission granted/denied
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)

        setupUI()
        setupSwitches()
        observeState()
        checkPermissions()
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun setupUI() {
        binding.btnGrantPermission.setOnClickListener {
            requestOverlayPermission()
        }

        binding.btnMasterToggle.setOnClickListener {
            if (!hasOverlayPermission()) {
                requestOverlayPermission()
                return@setOnClickListener
            }

            if (DarkrStateManager.isServiceRunning.value) {
                stopOverlayService()
            } else {
                startOverlayService()
            }
        }
    }

    private fun setupSwitches() {
        binding.switchShield.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitchesProgrammatically) return@setOnCheckedChangeListener
            prefs.isShieldEnabled = isChecked
            if (DarkrStateManager.isServiceRunning.value) {
                sendServiceAction(DarkrOverlayService.ACTION_TOGGLE_SHIELD)
            }
        }

        binding.switchBlackout.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitchesProgrammatically) return@setOnCheckedChangeListener
            prefs.isBlackoutEnabled = isChecked
            if (DarkrStateManager.isServiceRunning.value) {
                sendServiceAction(DarkrOverlayService.ACTION_TOGGLE_BLACKOUT)
            }
        }

        binding.switchFreeze.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitchesProgrammatically) return@setOnCheckedChangeListener
            prefs.isFreezeEnabled = isChecked
            if (DarkrStateManager.isServiceRunning.value) {
                sendServiceAction(DarkrOverlayService.ACTION_TOGGLE_FREEZE)
            }
        }

        binding.switchPanic.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitchesProgrammatically) return@setOnCheckedChangeListener
            prefs.isPanicEnabled = isChecked
            DarkrStateManager.setShakeEnabled(isChecked)
            if (DarkrStateManager.isServiceRunning.value) {
                // Refresh shake detector
                val intent = Intent(this, DarkrOverlayService::class.java).apply {
                    action = DarkrOverlayService.ACTION_START
                }
                startService(intent)
            }
        }

        binding.switchDimmer.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitchesProgrammatically) return@setOnCheckedChangeListener
            prefs.isDimmerEnabled = isChecked
            if (DarkrStateManager.isServiceRunning.value) {
                sendServiceAction(DarkrOverlayService.ACTION_TOGGLE_DIMMER)
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    DarkrStateManager.isServiceRunning.collect { isRunning ->
                        updateMasterServiceUI(isRunning)
                    }
                }
                launch {
                    DarkrStateManager.isShieldActive.collect { isActive ->
                        updateSwitchState(binding.switchShield, isActive)
                    }
                }
                launch {
                    DarkrStateManager.isBlackoutActive.collect { isActive ->
                        updateSwitchState(binding.switchBlackout, isActive)
                    }
                }
                launch {
                    DarkrStateManager.isFreezeActive.collect { isActive ->
                        updateSwitchState(binding.switchFreeze, isActive)
                    }
                }
                launch {
                    DarkrStateManager.isDimmerActive.collect { isActive ->
                        updateSwitchState(binding.switchDimmer, isActive)
                    }
                }
                launch {
                    DarkrStateManager.isShakeEnabled.collect { isEnabled ->
                        updateSwitchState(binding.switchPanic, isEnabled)
                    }
                }
            }
        }
    }

    private fun updateSwitchState(switch: com.google.android.material.switchmaterial.SwitchMaterial, state: Boolean) {
        if (switch.isChecked != state) {
            isUpdatingSwitchesProgrammatically = true
            switch.isChecked = state
            isUpdatingSwitchesProgrammatically = false
        }
    }

    private fun updateMasterServiceUI(isRunning: Boolean) {
        if (isRunning) {
            binding.tvStatusBadge.text = getString(R.string.status_active)
            binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_active)
            binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.white_pure))
            binding.btnMasterToggle.text = getString(R.string.btn_stop_service)
        } else {
            binding.tvStatusBadge.text = getString(R.string.status_inactive)
            binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_inactive)
            binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
            binding.btnMasterToggle.text = getString(R.string.btn_start_service)
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun checkPermissions() {
        val hasOverlay = hasOverlayPermission()
        binding.cardPermission.visibility = if (hasOverlay) View.GONE else View.VISIBLE
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, DarkrOverlayService::class.java).apply {
            action = DarkrOverlayService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
        prefs.isServiceEnabled = true
    }

    private fun stopOverlayService() {
        val intent = Intent(this, DarkrOverlayService::class.java).apply {
            action = DarkrOverlayService.ACTION_STOP
        }
        startService(intent)
        prefs.isServiceEnabled = false
    }

    private fun sendServiceAction(action: String) {
        val intent = Intent(this, DarkrOverlayService::class.java).apply {
            this.action = action
        }
        startService(intent)
    }
}

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
 * Darkr 2026 Primary Dashboard Activity.
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

        binding.btnOpenAppSettings.setOnClickListener {
            openAppDetailsSettings()
        }

        // Dominant Primary Action: BLACKEN SCREEN
        binding.btnBlackenScreen.setOnClickListener {
            if (!hasOverlayPermission()) {
                requestOverlayPermission()
                return@setOnClickListener
            }

            ensureServiceStarted()
            sendServiceAction(DarkrOverlayService.ACTION_TOGGLE_BLACKOUT)
        }

        // Floating Ghost Orb Switch
        binding.switchFloatingOrb.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitchesProgrammatically) return@setOnCheckedChangeListener
            if (isChecked) {
                if (!hasOverlayPermission()) {
                    requestOverlayPermission()
                    binding.switchFloatingOrb.isChecked = false
                    return@setOnCheckedChangeListener
                }
                startOverlayService()
            } else {
                stopOverlayService()
            }
        }
    }

    private fun setupSwitches() {
        // Clock Mode Switch
        binding.switchClock.isChecked = prefs.isClockEnabled
        binding.switchClock.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitchesProgrammatically) return@setOnCheckedChangeListener
            prefs.isClockEnabled = isChecked
            DarkrStateManager.setClockActive(isChecked)
        }

        // Pocket Mode Switch
        binding.switchPocket.isChecked = prefs.isPocketEnabled
        binding.switchPocket.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitchesProgrammatically) return@setOnCheckedChangeListener
            prefs.isPocketEnabled = isChecked
            DarkrStateManager.setPocketEnabled(isChecked)
            if (DarkrStateManager.isServiceRunning.value) {
                sendServiceAction(DarkrOverlayService.ACTION_REFRESH_SENSORS)
            }
        }

        // Privacy Curtain Switch
        binding.switchShield.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitchesProgrammatically) return@setOnCheckedChangeListener
            prefs.isShieldEnabled = isChecked
            if (DarkrStateManager.isServiceRunning.value) {
                sendServiceAction(DarkrOverlayService.ACTION_TOGGLE_SHIELD)
            } else if (isChecked) {
                ensureServiceStarted()
                sendServiceAction(DarkrOverlayService.ACTION_TOGGLE_SHIELD)
            }
        }

        // Touch Freeze Switch
        binding.switchFreeze.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitchesProgrammatically) return@setOnCheckedChangeListener
            prefs.isFreezeEnabled = isChecked
            if (DarkrStateManager.isServiceRunning.value) {
                sendServiceAction(DarkrOverlayService.ACTION_TOGGLE_FREEZE)
            } else if (isChecked) {
                ensureServiceStarted()
                sendServiceAction(DarkrOverlayService.ACTION_TOGGLE_FREEZE)
            }
        }

        // Shake Panic Switch
        binding.switchPanic.isChecked = prefs.isPanicEnabled
        binding.switchPanic.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitchesProgrammatically) return@setOnCheckedChangeListener
            prefs.isPanicEnabled = isChecked
            DarkrStateManager.setShakeEnabled(isChecked)
            if (DarkrStateManager.isServiceRunning.value) {
                sendServiceAction(DarkrOverlayService.ACTION_REFRESH_SENSORS)
            }
        }

        // Midnight Dimmer Switch
        binding.switchDimmer.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitchesProgrammatically) return@setOnCheckedChangeListener
            prefs.isDimmerEnabled = isChecked
            if (DarkrStateManager.isServiceRunning.value) {
                sendServiceAction(DarkrOverlayService.ACTION_TOGGLE_DIMMER)
            } else if (isChecked) {
                ensureServiceStarted()
                sendServiceAction(DarkrOverlayService.ACTION_TOGGLE_DIMMER)
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    DarkrStateManager.isServiceRunning.collect { isRunning ->
                        updateSwitchState(binding.switchFloatingOrb, isRunning)
                        updateTelemetryUI()
                    }
                }
                launch {
                    DarkrStateManager.isBlackoutActive.collect { isBlackout ->
                        updateBlackoutCTA(isBlackout)
                        updateTelemetryUI()
                    }
                }
                launch {
                    DarkrStateManager.isShieldActive.collect { isActive ->
                        updateSwitchState(binding.switchShield, isActive)
                        updateTelemetryUI()
                    }
                }
                launch {
                    DarkrStateManager.isFreezeActive.collect { isActive ->
                        updateSwitchState(binding.switchFreeze, isActive)
                        updateTelemetryUI()
                    }
                }
                launch {
                    DarkrStateManager.isDimmerActive.collect { isActive ->
                        updateSwitchState(binding.switchDimmer, isActive)
                        updateTelemetryUI()
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

    private fun updateBlackoutCTA(isBlackout: Boolean) {
        if (isBlackout) {
            binding.btnBlackenScreen.text = "RESTORE SCREEN"
            binding.btnBlackenScreen.setBackgroundColor(ContextCompat.getColor(this, R.color.card_dark_elevated))
            binding.btnBlackenScreen.setTextColor(ContextCompat.getColor(this, R.color.white_pure))
        } else {
            binding.btnBlackenScreen.text = "BLACKEN SCREEN"
            binding.btnBlackenScreen.setBackgroundColor(ContextCompat.getColor(this, R.color.white_pure))
            binding.btnBlackenScreen.setTextColor(ContextCompat.getColor(this, R.color.black_true))
        }
    }

    private fun updateTelemetryUI() {
        val isBlackout = DarkrStateManager.isBlackoutActive.value
        val isServiceRunning = DarkrStateManager.isServiceRunning.value
        val isShield = DarkrStateManager.isShieldActive.value

        when {
            isBlackout -> {
                binding.tvStatusBadge.text = "BLACKOUT ACTIVE"
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_active)
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.white_pure))
            }
            isShield -> {
                binding.tvStatusBadge.text = "CURTAIN ACTIVE"
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_active)
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.white_pure))
            }
            isServiceRunning -> {
                binding.tvStatusBadge.text = "GHOST ORB ON"
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_active)
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.white_pure))
            }
            else -> {
                binding.tvStatusBadge.text = "READY"
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_inactive)
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
            }
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
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            } catch (e: Exception) {
                openAppDetailsSettings()
            }
        }
    }

    private fun openAppDetailsSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            Toast.makeText(this, "Tap 3 dots (⋮) in top right & select 'Allow restricted settings'", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open settings", Toast.LENGTH_SHORT).show()
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

    private fun ensureServiceStarted() {
        if (!DarkrStateManager.isServiceRunning.value) {
            startOverlayService()
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

package com.darkr.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.darkr.app.databinding.ActivityMainBinding
import com.darkr.app.service.DarkrOverlayService
import com.darkr.app.util.PreferencesManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkOverlayPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)

        setupUI()
        setupSwitches()
        checkOverlayPermission()
    }

    override fun onResume() {
        super.onResume()
        checkOverlayPermission()
        updateServiceStatusUI()
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

            if (DarkrOverlayService.isRunning) {
                stopOverlayService()
            } else {
                startOverlayService()
            }
        }
    }

    private fun setupSwitches() {
        binding.switchShield.isChecked = prefs.isShieldEnabled
        binding.switchShield.setOnCheckedChangeListener { _, isChecked ->
            prefs.isShieldEnabled = isChecked
            if (DarkrOverlayService.isRunning && isChecked) {
                sendServiceAction(DarkrOverlayService.ACTION_TOGGLE_SHIELD)
            }
        }

        binding.switchBlackout.isChecked = prefs.isBlackoutEnabled
        binding.switchBlackout.setOnCheckedChangeListener { _, isChecked ->
            prefs.isBlackoutEnabled = isChecked
            if (DarkrOverlayService.isRunning && isChecked) {
                sendServiceAction(DarkrOverlayService.ACTION_TOGGLE_BLACKOUT)
            }
        }

        binding.switchFreeze.isChecked = prefs.isFreezeEnabled
        binding.switchFreeze.setOnCheckedChangeListener { _, isChecked ->
            prefs.isFreezeEnabled = isChecked
            if (DarkrOverlayService.isRunning && isChecked) {
                sendServiceAction(DarkrOverlayService.ACTION_TOGGLE_FREEZE)
            }
        }

        binding.switchPanic.isChecked = prefs.isPanicEnabled
        binding.switchPanic.setOnCheckedChangeListener { _, isChecked ->
            prefs.isPanicEnabled = isChecked
            Toast.makeText(
                this,
                if (isChecked) "Shake gesture active" else "Shake gesture disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.switchDimmer.isChecked = prefs.isDimmerEnabled
        binding.switchDimmer.setOnCheckedChangeListener { _, isChecked ->
            prefs.isDimmerEnabled = isChecked
            if (DarkrOverlayService.isRunning) {
                sendServiceAction(DarkrOverlayService.ACTION_TOGGLE_DIMMER)
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

    private fun checkOverlayPermission() {
        val hasPermission = hasOverlayPermission()
        binding.cardPermission.visibility = if (hasPermission) View.GONE else View.VISIBLE
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
        updateServiceStatusUI()
        Toast.makeText(this, "Darkr Floating Pill Launched!", Toast.LENGTH_SHORT).show()
    }

    private fun stopOverlayService() {
        val intent = Intent(this, DarkrOverlayService::class.java).apply {
            action = DarkrOverlayService.ACTION_STOP
        }
        startService(intent)
        prefs.isServiceEnabled = false
        updateServiceStatusUI()
        Toast.makeText(this, "Darkr Stopped", Toast.LENGTH_SHORT).show()
    }

    private fun sendServiceAction(action: String) {
        val intent = Intent(this, DarkrOverlayService::class.java).apply {
            this.action = action
        }
        startService(intent)
    }

    private fun updateServiceStatusUI() {
        val isRunning = DarkrOverlayService.isRunning
        if (isRunning) {
            binding.tvStatusBadge.text = getString(R.string.status_active)
            binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_active)
            binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.emerald_accent))
            binding.btnMasterToggle.text = getString(R.string.btn_stop_service)
        } else {
            binding.tvStatusBadge.text = getString(R.string.status_inactive)
            binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_inactive)
            binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
            binding.btnMasterToggle.text = getString(R.string.btn_start_service)
        }
    }
}

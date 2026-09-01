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
import com.darkr.app.util.StatsManager
import kotlinx.coroutines.launch

/**
 * Darkr 3-Tab Primary Dashboard Activity (Home, Stats, Settings).
 * Synchronizes real-time state with DarkrStateManager, StatsManager, and DarkrOverlayService.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var statsManager: StatsManager

    private var currentTab = TAB_HOME
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
        statsManager = StatsManager(this)

        setupNavigationTabs()
        setupUI()
        setupSettingsSwitches()
        observeState()
        checkPermissions()
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        refreshStats()
    }

    private fun setupNavigationTabs() {
        binding.navTabHome.setOnClickListener { switchTab(TAB_HOME) }
        binding.navTabStats.setOnClickListener { switchTab(TAB_STATS) }
        binding.navTabSettings.setOnClickListener { switchTab(TAB_SETTINGS) }
    }

    private fun switchTab(tab: Int) {
        currentTab = tab

        binding.layoutTabHome.visibility = if (tab == TAB_HOME) View.VISIBLE else View.GONE
        binding.layoutTabStats.visibility = if (tab == TAB_STATS) View.VISIBLE else View.GONE
        binding.layoutTabSettings.visibility = if (tab == TAB_SETTINGS) View.VISIBLE else View.GONE

        val colorActive = ContextCompat.getColor(this, R.color.white_pure)
        val colorInactive = ContextCompat.getColor(this, R.color.text_muted)

        // Tab 1: Home
        binding.imgNavHome.setColorFilter(if (tab == TAB_HOME) colorActive else colorInactive)
        binding.tvNavHome.setTextColor(if (tab == TAB_HOME) colorActive else colorInactive)

        // Tab 2: Stats
        binding.imgNavStats.setColorFilter(if (tab == TAB_STATS) colorActive else colorInactive)
        binding.tvNavStats.setTextColor(if (tab == TAB_STATS) colorActive else colorInactive)
        if (tab == TAB_STATS) refreshStats()

        // Tab 3: Settings
        binding.imgNavSettings.setColorFilter(if (tab == TAB_SETTINGS) colorActive else colorInactive)
        binding.tvNavSettings.setTextColor(if (tab == TAB_SETTINGS) colorActive else colorInactive)
    }

    private fun setupUI() {
        binding.btnGrantPermission.setOnClickListener {
            requestOverlayPermission()
        }

        binding.btnOpenAppSettings.setOnClickListener {
            openAppDetailsSettings()
        }

        // Master Floating Service Start/Stop Button
        binding.btnMasterServiceCircle.setOnClickListener {
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

        // Direct Blacken Screen CTA
        binding.btnBlackenScreenNow.setOnClickListener {
            if (!hasOverlayPermission()) {
                requestOverlayPermission()
                return@setOnClickListener
            }

            ensureServiceStarted()
            sendServiceAction(DarkrOverlayService.ACTION_TOGGLE_BLACKOUT)
        }

        // Top-Right 3-Dots More Menu
        binding.btnMenuMore.setOnClickListener { view ->
            showMoreMenu(view)
        }
    }

    private fun showMoreMenu(anchor: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "About Darkr")
        popup.menu.add(0, 2, 1, "Help & FAQ")
        popup.menu.add(0, 3, 2, "Reset Statistics")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> showAboutDialog()
                2 -> showHelpDialog()
                3 -> resetStatistics()
            }
            true
        }
        popup.show()
    }

    private fun showAboutDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("About Darkr")
            .setMessage(
                "Darkr v2.0.0 (Production Release)\n\n" +
                "Best-in-class, privacy-first screen blackout utility for OLED/AMOLED displays.\n\n" +
                "• 100% Free Forever\n" +
                "• Zero Ads & Zero Tracking\n" +
                "• 100% Offline (No Internet Permission)\n" +
                "• Open Source under Apache 2.0\n\n" +
                "Repository:\nhttps://github.com/hashmiii14/Darkr"
            )
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showHelpDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Darkr Help & FAQ")
            .setMessage(
                "1. How to use with YouTube / Spotify:\n" +
                "Start your video or audio, then tap the Floating Ghost Orb or 'BLACKEN SCREEN NOW' to turn your screen completely black while audio keeps playing.\n\n" +
                "2. How to unlock:\n" +
                "Tap anywhere on the black screen to reveal the bottom 'Tap to Unlock' button, then tap it to restore the screen.\n\n" +
                "3. AMOLED Battery Savings:\n" +
                "On OLED/AMOLED screens, pure black pixels physically turn off, saving up to 60-80% of display battery."
            )
            .setPositiveButton("Got It", null)
            .show()
    }

    private fun resetStatistics() {
        getSharedPreferences("darkr_stats_prefs", MODE_PRIVATE).edit().clear().apply()
        refreshStats()
        Toast.makeText(this, "Statistics reset successfully", Toast.LENGTH_SHORT).show()
    }

    private fun setupSettingsSwitches() {
        // Clock Switch
        binding.switchClock.isChecked = prefs.isClockEnabled
        binding.switchClock.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitchesProgrammatically) return@setOnCheckedChangeListener
            prefs.isClockEnabled = isChecked
            DarkrStateManager.setClockActive(isChecked)
        }

        // 24-Hour Switch
        binding.switch24Hour.isChecked = prefs.isTime24Hour
        binding.switch24Hour.setOnCheckedChangeListener { _, isChecked ->
            prefs.isTime24Hour = isChecked
        }

        // Show Date Switch
        binding.switchShowDate.isChecked = prefs.isShowDate
        binding.switchShowDate.setOnCheckedChangeListener { _, isChecked ->
            prefs.isShowDate = isChecked
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

        // Haptic Vibration Switch
        binding.switchVibration.isChecked = prefs.isVibrationEnabled
        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.isVibrationEnabled = isChecked
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    DarkrStateManager.isServiceRunning.collect { isRunning ->
                        updateMasterServiceUI(isRunning)
                        updateTelemetryUI()
                    }
                }
                launch {
                    DarkrStateManager.isBlackoutActive.collect { isBlackout ->
                        updateBlackoutCTA(isBlackout)
                        updateTelemetryUI()
                        if (!isBlackout) refreshStats()
                    }
                }
            }
        }
    }

    private fun updateMasterServiceUI(isRunning: Boolean) {
        if (isRunning) {
            binding.tvServiceButtonText.text = "STOP"
            binding.tvServiceButtonText.setTextColor(ContextCompat.getColor(this, R.color.white_pure))
            binding.tvServiceSubtitle.text = "Floating Ghost Orb is active over apps"
            binding.imgServiceStateIcon.setImageResource(R.drawable.ic_blackout)
        } else {
            binding.tvServiceButtonText.text = "START"
            binding.tvServiceButtonText.setTextColor(ContextCompat.getColor(this, R.color.white_pure))
            binding.tvServiceSubtitle.text = "Tap START to enable Floating Ghost Orb"
            binding.imgServiceStateIcon.setImageResource(R.drawable.ic_darkr_logo)
        }
    }

    private fun updateBlackoutCTA(isBlackout: Boolean) {
        if (isBlackout) {
            binding.btnBlackenScreenNow.text = "RESTORE SCREEN"
            binding.btnBlackenScreenNow.setBackgroundColor(ContextCompat.getColor(this, R.color.card_dark_elevated))
            binding.btnBlackenScreenNow.setTextColor(ContextCompat.getColor(this, R.color.white_pure))
        } else {
            binding.btnBlackenScreenNow.text = "BLACKEN SCREEN NOW"
            binding.btnBlackenScreenNow.setBackgroundColor(ContextCompat.getColor(this, R.color.white_pure))
            binding.btnBlackenScreenNow.setTextColor(ContextCompat.getColor(this, R.color.black_true))
        }
    }

    private fun updateTelemetryUI() {
        val isBlackout = DarkrStateManager.isBlackoutActive.value
        val isServiceRunning = DarkrStateManager.isServiceRunning.value

        when {
            isBlackout -> {
                binding.tvStatusBadge.text = "BLACKOUT ACTIVE"
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

    private fun refreshStats() {
        val todaySecs = statsManager.getTodayBlackoutSeconds()
        val totalSecs = statsManager.getTotalBlackoutSeconds()
        val totalSessions = statsManager.getTotalSessions()
        val savedPercent = statsManager.getEstimatedBatterySavedPercent()
        val savedMah = statsManager.getEstimatedBatterySavedMah()

        binding.tvStatsBatterySaved.text = "~$savedPercent% Saved ($savedMah mAh)"
        binding.tvStatsTodayDuration.text = statsManager.formatDuration(todaySecs)
        binding.tvStatsTotalDuration.text = statsManager.formatDuration(totalSecs)
        binding.tvStatsTotalSessions.text = "$totalSessions Blackout Sessions"
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

    companion object {
        private const val TAB_HOME = 0
        private const val TAB_STATS = 1
        private const val TAB_SETTINGS = 2
    }
}

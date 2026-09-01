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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Darkr Primary Dashboard Activity.
 * Pixel-perfect implementation matching Black Screen by japp.io layout:
 * - Top header with Darkr title and 3-dots menu
 * - Center interactive Phone Mockup preview frame
 * - 3 Sub-tabs (Look & Feel | Settings | Stats)
 * - Prominent bottom "Start" / "Stop" button
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var statsManager: StatsManager

    private var currentTab = TAB_LOOK_FEEL
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
        updateMockupPreview()
        observeState()
        checkPermissions()
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        updateMockupPreview()
        refreshStats()
    }

    private fun setupNavigationTabs() {
        binding.tabBtnLookFeel.setOnClickListener { switchTab(TAB_LOOK_FEEL) }
        binding.tabBtnSettings.setOnClickListener { switchTab(TAB_SETTINGS) }
        binding.tabBtnStats.setOnClickListener { switchTab(TAB_STATS) }
    }

    private fun switchTab(tab: Int) {
        currentTab = tab

        binding.contentLookFeel.visibility = if (tab == TAB_LOOK_FEEL) View.VISIBLE else View.GONE
        binding.contentSettings.visibility = if (tab == TAB_SETTINGS) View.VISIBLE else View.GONE
        binding.contentStats.visibility = if (tab == TAB_STATS) View.VISIBLE else View.GONE

        val colorActive = ContextCompat.getColor(this, R.color.white_pure)
        val colorInactive = ContextCompat.getColor(this, R.color.text_muted)

        // Tab 1: Look & Feel
        binding.tvTabLookFeel.setTextColor(if (tab == TAB_LOOK_FEEL) colorActive else colorInactive)
        binding.indicatorLookFeel.visibility = if (tab == TAB_LOOK_FEEL) View.VISIBLE else View.INVISIBLE

        // Tab 2: Settings
        binding.tvTabSettings.setTextColor(if (tab == TAB_SETTINGS) colorActive else colorInactive)
        binding.indicatorSettings.visibility = if (tab == TAB_SETTINGS) View.VISIBLE else View.INVISIBLE

        // Tab 3: Stats
        binding.tvTabStats.setTextColor(if (tab == TAB_STATS) colorActive else colorInactive)
        binding.indicatorStats.visibility = if (tab == TAB_STATS) View.VISIBLE else View.INVISIBLE
        if (tab == TAB_STATS) refreshStats()
    }

    private fun setupUI() {
        binding.btnGrantPermission.setOnClickListener {
            requestOverlayPermission()
        }

        binding.btnOpenAppSettings.setOnClickListener {
            openAppDetailsSettings()
        }

        // Bottom Master Start/Stop Button (Matching Image 3)
        binding.btnMasterBottomAction.setOnClickListener {
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
            triggerBlackoutDirectly()
        }

        // Tapping the Phone Mockup preview also triggers blackout directly
        binding.cardPhoneMockup.setOnClickListener {
            triggerBlackoutDirectly()
        }

        // Mode Cards Selection
        binding.cardModeFullScreen.setOnClickListener {
            binding.cardModeFullScreen.setBackgroundResource(R.drawable.bg_card_selected)
            binding.cardModePrivacy.setBackgroundResource(R.drawable.bg_card_dark)
        }

        binding.cardModePrivacy.setOnClickListener {
            binding.cardModeFullScreen.setBackgroundResource(R.drawable.bg_card_dark)
            binding.cardModePrivacy.setBackgroundResource(R.drawable.bg_card_selected)
            if (hasOverlayPermission()) {
                ensureServiceStarted()
                sendServiceAction(DarkrOverlayService.ACTION_TOGGLE_SHIELD)
            }
        }

        // Clock Style Selection
        binding.cardStyleStandard.setOnClickListener {
            binding.cardStyleStandard.setBackgroundResource(R.drawable.bg_card_selected)
            binding.cardStyleBold.setBackgroundResource(R.drawable.bg_card_dark)
        }

        binding.cardStyleBold.setOnClickListener {
            binding.cardStyleStandard.setBackgroundResource(R.drawable.bg_card_dark)
            binding.cardStyleBold.setBackgroundResource(R.drawable.bg_card_selected)
        }

        // Top-Right 3-Dots Menu
        binding.btnMenuMore.setOnClickListener { view ->
            showMoreMenu(view)
        }
    }

    private fun triggerBlackoutDirectly() {
        if (!hasOverlayPermission()) {
            requestOverlayPermission()
            return
        }

        ensureServiceStarted()
        sendServiceAction(DarkrOverlayService.ACTION_TOGGLE_BLACKOUT)
    }

    private fun setupSettingsSwitches() {
        // Clock Switch
        binding.switchClock.isChecked = prefs.isClockEnabled
        binding.switchClock.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitchesProgrammatically) return@setOnCheckedChangeListener
            prefs.isClockEnabled = isChecked
            DarkrStateManager.setClockActive(isChecked)
            updateMockupPreview()
        }

        // 24-Hour Switch
        binding.switch24Hour.isChecked = prefs.isTime24Hour
        binding.switch24Hour.setOnCheckedChangeListener { _, isChecked ->
            prefs.isTime24Hour = isChecked
            updateMockupPreview()
        }

        // Show Date Switch
        binding.switchShowDate.isChecked = prefs.isShowDate
        binding.switchShowDate.setOnCheckedChangeListener { _, isChecked ->
            prefs.isShowDate = isChecked
            updateMockupPreview()
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

    private fun updateMockupPreview() {
        val now = Date()
        val pattern = if (prefs.isTime24Hour) "HH:mm" else "h:mm"
        val timeFormat = SimpleDateFormat(pattern, Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

        binding.tvMockupTime.text = timeFormat.format(now)
        binding.tvMockupDate.text = dateFormat.format(now)
        binding.tvMockupDate.visibility = if (prefs.isShowDate) View.VISIBLE else View.GONE
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
                    DarkrStateManager.isBlackoutActive.collect { isBlackout ->
                        updateBlackoutCTA(isBlackout)
                        if (!isBlackout) refreshStats()
                    }
                }
            }
        }
    }

    private fun updateMasterServiceUI(isRunning: Boolean) {
        if (isRunning) {
            binding.btnMasterBottomAction.text = "Stop"
            binding.btnMasterBottomAction.setBackgroundColor(ContextCompat.getColor(this, R.color.card_dark_elevated))
            binding.btnMasterBottomAction.setTextColor(ContextCompat.getColor(this, R.color.white_pure))
        } else {
            binding.btnMasterBottomAction.text = "Start"
            binding.btnMasterBottomAction.setBackgroundColor(ContextCompat.getColor(this, R.color.white_pure))
            binding.btnMasterBottomAction.setTextColor(ContextCompat.getColor(this, R.color.black_true))
        }
    }

    private fun updateBlackoutCTA(isBlackout: Boolean) {
        if (isBlackout) {
            binding.btnBlackenScreenNow.text = "Restore Screen"
        } else {
            binding.btnBlackenScreenNow.text = "Blacken Screen Now"
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
        binding.tvStatsTotalSessions.text = "$totalSessions Sessions"
    }

    private fun showMoreMenu(anchor: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "About Darkr")
        popup.menu.add(0, 2, 1, "Help")
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
                "Best-in-class AMOLED screen blackout utility.\n\n" +
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
            .setTitle("Help & Info")
            .setMessage(
                "1. How to use with YouTube / Spotify:\n" +
                "Start media, tap the floating button or 'Blacken Screen Now' to black out your screen while audio continues.\n\n" +
                "2. How to unlock:\n" +
                "Tap anywhere on the black screen to highlight the bottom 'UNLOCK' button, then tap UNLOCK.\n\n" +
                "3. Battery Savings:\n" +
                "On OLED/AMOLED screens, pure black pixels physically turn off, saving up to 60-80% display power."
            )
            .setPositiveButton("Got It", null)
            .show()
    }

    private fun resetStatistics() {
        getSharedPreferences("darkr_stats_prefs", MODE_PRIVATE).edit().clear().apply()
        refreshStats()
        Toast.makeText(this, "Statistics reset successfully", Toast.LENGTH_SHORT).show()
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
        private const val TAB_LOOK_FEEL = 0
        private const val TAB_SETTINGS = 1
        private const val TAB_STATS = 2
    }
}

package com.gameperf.assistant

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.gameperf.assistant.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startGameModeService()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.switchGameMode.isChecked = PrefsHelper.isGameModeOn(this)

        binding.switchGameMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) requestPermissionsAndStart() else stopGameModeService()
            PrefsHelper.setGameModeOn(this, isChecked)
        }

        binding.buttonSelectApps.setOnClickListener {
            startActivity(Intent(this, AppListActivity::class.java))
        }

        binding.buttonRefreshStats.setOnClickListener { refreshStats() }

        binding.buttonApplyRefreshRate.setOnClickListener {
            val applied = RefreshRateHelper.applyHighestRefreshRateToOwnWindow(this)
            binding.textRefreshRateResult.text = if (applied != null) {
                getString(R.string.refresh_rate_applied_format, applied)
            } else {
                getString(R.string.refresh_rate_unavailable)
            }
        }

        binding.buttonOpenDisplaySettings.setOnClickListener {
            RefreshRateHelper.openSystemDisplaySettings(this)
        }

        binding.buttonOpenNotificationSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }

        binding.buttonLaunchGame.setOnClickListener { launchPubgIfInstalled() }

        showSupportedRefreshRates()
        refreshStats()
        updateLaunchButtonVisibility()
    }

    override fun onResume() {
        super.onResume()
        refreshStats()
        updateDndHintVisibility()
    }

    private fun requestPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startGameModeService()
        }
    }

    private fun startGameModeService() {
        val intent = Intent(this, GameModeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        updateDndHintVisibility()
    }

    private fun stopGameModeService() {
        stopService(Intent(this, GameModeService::class.java))
    }

    private fun updateDndHintVisibility() {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        binding.textDndHint.visibility = if (!notificationManager.isNotificationPolicyAccessGranted) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun refreshStats() {
        val snapshot = PerformanceMonitor.read(this)
        binding.textRam.text = getString(
            R.string.ram_format, snapshot.availableRamMb, snapshot.totalRamMb
        )
        binding.textBattery.text = getString(
            R.string.battery_format,
            snapshot.batteryPercent,
            snapshot.batteryTempCelsius ?: -1f
        )
        binding.textThermal.text = snapshot.thermalStatusText
        binding.textCpu.text = getString(
            R.string.cpu_format, snapshot.cpuCoreCount, snapshot.cpuFreqText
        )
    }

    private fun showSupportedRefreshRates() {
        val modes = RefreshRateHelper.getSupportedModes(this)
        binding.textSupportedModes.text = if (modes.isEmpty()) {
            getString(R.string.refresh_rate_unavailable)
        } else {
            modes.joinToString("\n") { "${it.width}x${it.height} @ ${it.refreshRate.toInt()} Hz" }
        }
    }

    private fun candidatePubgPackages() = listOf(
        "com.tencent.ig",
        "com.pubg.krmobile",
        "com.vng.pubgmobile",
        "com.rekoo.pubgm"
    )

    private fun installedPubgPackage(): String? {
        val pm = packageManager
        return candidatePubgPackages().firstOrNull { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    private fun updateLaunchButtonVisibility() {
        binding.buttonLaunchGame.visibility = if (installedPubgPackage() != null) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun launchPubgIfInstalled() {
        val pkg = installedPubgPackage() ?: return
        packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
    }
}

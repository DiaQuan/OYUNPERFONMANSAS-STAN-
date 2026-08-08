package com.gameperf.assistant

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import java.io.File

/**
 * Cihazın anlık kaynak durumunu okuyan yardımcı sınıf.
 *
 * Tüm okumalar Android'in resmi, genel API'leri üzerinden yapılır.
 * Root erişimi veya izinsiz sistem dosyası okuması kullanılmaz.
 * Hiçbir oyunun dosyasına, config'ine veya belleğine dokunulmaz.
 */
object PerformanceMonitor {

    data class Snapshot(
        val availableRamMb: Long,
        val totalRamMb: Long,
        val batteryPercent: Int,
        val batteryTempCelsius: Float?,
        val thermalStatusText: String,
        val cpuCoreCount: Int,
        val cpuFreqText: String
    )

    fun read(context: Context): Snapshot {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val availableRamMb = memInfo.availMem / (1024 * 1024)
        val totalRamMb = memInfo.totalMem / (1024 * 1024)

        val batteryIntent = context.applicationContext.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPercent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val batteryTemp = if (tempTenths != null && tempTenths != Int.MIN_VALUE) tempTenths / 10f else null

        val thermalStatusText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            when (pm.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "Normal"
                PowerManager.THERMAL_STATUS_LIGHT -> "Hafif Isınma"
                PowerManager.THERMAL_STATUS_MODERATE -> "Orta Isınma"
                PowerManager.THERMAL_STATUS_SEVERE -> "Yüksek Isınma"
                PowerManager.THERMAL_STATUS_CRITICAL -> "Kritik Isınma"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "Acil Durum"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "Kapanma Riski"
                else -> "Bilinmiyor"
            }
        } else {
            "Bu Android sürümünde desteklenmiyor"
        }

        val cpuCoreCount = Runtime.getRuntime().availableProcessors()
        val cpuFreqText = readCpuFreqSafely(cpuCoreCount)

        return Snapshot(
            availableRamMb = availableRamMb,
            totalRamMb = totalRamMb,
            batteryPercent = batteryPercent,
            batteryTempCelsius = batteryTemp,
            thermalStatusText = thermalStatusText,
            cpuCoreCount = cpuCoreCount,
            cpuFreqText = cpuFreqText
        )
    }

    /**
     * Bazı cihazlarda /sys/devices/system/cpu/... okunabilir, bazı üreticiler
     * bu dosyalara erişimi kısıtlar. Okunamazsa uygulamayı çökertmeden
     * "desteklenmiyor" metni döner.
     */
    private fun readCpuFreqSafely(coreCount: Int): String {
        return try {
            val freqs = mutableListOf<Long>()
            for (i in 0 until coreCount) {
                val file = File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
                if (file.exists() && file.canRead()) {
                    val khz = file.readText().trim().toLongOrNull()
                    if (khz != null) freqs.add(khz / 1000)
                }
            }
            if (freqs.isEmpty()) "Okunamıyor (üretici kısıtlaması olabilir)"
            else "${freqs.max()} MHz (en yüksek çekirdek)"
        } catch (e: Exception) {
            "Okunamıyor (üretici kısıtlaması olabilir)"
        }
    }
}

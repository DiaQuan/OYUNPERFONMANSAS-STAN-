package com.gameperf.assistant

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.Display

object RefreshRateHelper {

    data class ModeInfo(val modeId: Int, val width: Int, val height: Int, val refreshRate: Float)

    @Suppress("DEPRECATION")
    private fun getDisplay(activity: Activity): Display? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            activity.windowManager.defaultDisplay
        }
    }

    fun getSupportedModes(activity: Activity): List<ModeInfo> {
        val display = getDisplay(activity) ?: return emptyList()
        return display.supportedModes.map {
            ModeInfo(it.modeId, it.physicalWidth, it.physicalHeight, it.refreshRate)
        }.sortedByDescending { it.refreshRate }
    }

    /**
     * Bu uygulamanın KENDİ penceresi için en yüksek yenileme hızını tercih eder.
     *
     * ÖNEMLİ SINIR: Bu ayar sadece bu uygulama ön plandayken geçerlidir.
     * Başka bir uygulamanın (örn. PUBG) penceresini bu API ile zorlamak
     * mümkün değildir — Android, bir uygulamanın başka bir uygulamanın
     * render davranışını kontrol etmesine izin vermez. Sistem çapında bir
     * etki için kullanıcının kendisinin Ayarlar'daki yenileme hızı
     * seçeneğini açması gerekir (bkz. openSystemDisplaySettings).
     */
    fun applyHighestRefreshRateToOwnWindow(activity: Activity): Float? {
        val modes = getSupportedModes(activity)
        val best = modes.maxByOrNull { it.refreshRate } ?: return null
        val window = activity.window
        val params = window.attributes
        params.preferredDisplayModeId = best.modeId
        window.attributes = params
        return best.refreshRate
    }

    fun openSystemDisplaySettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
        activity.startActivity(intent)
    }

    fun openDeveloperOptions(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        try {
            activity.startActivity(intent)
        } catch (e: Exception) {
            openSystemDisplaySettings(activity)
        }
    }
}

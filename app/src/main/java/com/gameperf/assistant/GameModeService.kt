package com.gameperf.assistant

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

/**
 * Oyun Modu servisi.
 *
 * Yaptıkları (sadece bunlar):
 *  - Kullanıcı izin verdiyse Rahatsız Etme (DND) filtresini açar/kapatır
 *  - Seçilen uygulamaların arka plan önbelleğini temizlemeyi DENER
 *    (Android bu davranışı sınırlar, garanti değildir)
 *  - Canlı RAM/pil/termal durumunu bir bildirimde gösterir
 *
 * Yapmadıkları: Hiçbir oyunun dosyasına, config'ine, belleğine veya
 * sürecine dokunmaz. PUBG veya başka bir oyunu hiçbir şekilde değiştirmez.
 */
class GameModeService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var previousInterruptionFilter: Int = NotificationManager.INTERRUPTION_FILTER_ALL
    private lateinit var notificationManager: NotificationManager

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateNotification()
            trimSelectedApps()
            handler.postDelayed(this, UPDATE_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createChannelIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(PerformanceMonitor.read(this)))
        enableDoNotDisturbIfAllowed()
        handler.post(updateRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateRunnable)
        restoreInterruptionFilter()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.game_mode_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.game_mode_channel_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(snapshot: PerformanceMonitor.Snapshot) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_speakerphone)
            .setContentTitle(getString(R.string.game_mode_active_title))
            .setContentText(
                getString(
                    R.string.game_mode_notification_text,
                    snapshot.availableRamMb,
                    snapshot.batteryPercent,
                    snapshot.thermalStatusText
                )
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun updateNotification() {
        val snapshot = PerformanceMonitor.read(this)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(snapshot))
    }

    private fun enableDoNotDisturbIfAllowed() {
        if (notificationManager.isNotificationPolicyAccessGranted) {
            previousInterruptionFilter = notificationManager.currentInterruptionFilter
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        }
    }

    private fun restoreInterruptionFilter() {
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(previousInterruptionFilter)
        }
    }

    /**
     * Seçilen uygulamaların önbellekteki (cache) arka plan süreçlerini
     * temizlemeyi dener. Android; aktif/önemli süreçleri bu şekilde
     * kapatmaya izin vermez — bu yalnızca sistemin zaten kapatmaya uygun
     * gördüğü önbellek süreçleri için bir "ipucu"dur, garanti değildir.
     */
    private fun trimSelectedApps() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val selected = PrefsHelper.getSelectedApps(this@GameModeService)
        for (pkg in selected) {
            try {
                am.killBackgroundProcesses(pkg)
            } catch (e: SecurityException) {
                // İzin/güvenlik kısıtlaması varsa sessizce geç
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "game_mode_channel"
        private const val NOTIFICATION_ID = 42
        private const val UPDATE_INTERVAL_MS = 5000L
    }
}

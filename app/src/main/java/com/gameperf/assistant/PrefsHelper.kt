package com.gameperf.assistant

import android.content.Context

/**
 * Basit SharedPreferences sarmalayıcısı.
 * Sadece bu uygulamanın kendi tercihlerini (hedef uygulama listesi,
 * oyun modu açık/kapalı durumu) saklar.
 */
object PrefsHelper {
    private const val PREFS_NAME = "gameperf_prefs"
    private const val KEY_SELECTED_APPS = "selected_apps"
    private const val KEY_GAME_MODE_ON = "game_mode_on"

    fun getSelectedApps(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_SELECTED_APPS, emptySet()) ?: emptySet()
    }

    fun setSelectedApps(context: Context, packages: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_SELECTED_APPS, packages).apply()
    }

    fun isGameModeOn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_GAME_MODE_ON, false)
    }

    fun setGameModeOn(context: Context, on: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_GAME_MODE_ON, on).apply()
    }
}

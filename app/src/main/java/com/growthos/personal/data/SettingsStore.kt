package com.growthos.personal.data

import android.content.Context

class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun wakeEnabled(): Boolean = prefs.getBoolean("wake_enabled", false)

    fun setWakeEnabled(value: Boolean) {
        prefs.edit().putBoolean("wake_enabled", value).apply()
    }

    fun wakeMinute(): Int = prefs.getInt("wake_minute", 5 * 60)

    fun setWakeMinute(value: Int) {
        prefs.edit().putInt("wake_minute", value).apply()
    }

    fun themeMode(): Int = prefs.getInt("theme_mode", 0)

    fun setThemeMode(value: Int) {
        prefs.edit().putInt("theme_mode", value).apply()
    }

    fun reviewEnabled(): Boolean = prefs.getBoolean("review_enabled", false)

    fun setReviewEnabled(value: Boolean) {
        prefs.edit().putBoolean("review_enabled", value).apply()
    }

    fun reviewMinute(): Int = prefs.getInt("review_minute", 21 * 60)

    fun setReviewMinute(value: Int) {
        prefs.edit().putInt("review_minute", value).apply()
    }

    // epoch day on which the routine is paused (-1 = never)
    fun pausedDay(): Long = prefs.getLong("paused_day", -1L)

    fun setPausedDay(value: Long) {
        prefs.edit().putLong("paused_day", value).apply()
    }

    fun routineSeeded(): Boolean = prefs.getBoolean("routine_seeded", false)

    fun setRoutineSeeded() {
        prefs.edit().putBoolean("routine_seeded", true).apply()
    }
}

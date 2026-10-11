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

    fun routineSeeded(): Boolean = prefs.getBoolean("routine_seeded", false)

    fun setRoutineSeeded() {
        prefs.edit().putBoolean("routine_seeded", true).apply()
    }
}

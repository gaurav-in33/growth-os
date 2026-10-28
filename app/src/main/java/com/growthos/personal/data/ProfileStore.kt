package com.growthos.personal.data

import android.content.Context

class ProfileStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("profile", Context.MODE_PRIVATE)

    fun name(): String = prefs.getString("name", "") ?: ""

    fun setName(value: String) {
        prefs.edit().putString("name", value.trim()).apply()
    }

    fun isOnboardingDone(): Boolean = prefs.getBoolean("onboarding_done", false)

    fun setOnboardingDone() {
        prefs.edit().putBoolean("onboarding_done", true).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

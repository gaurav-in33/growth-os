package com.growthos.personal.data

import android.content.Context

class AiStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("ai", Context.MODE_PRIVATE)

    fun enabled(): Boolean = prefs.getBoolean("enabled", false)

    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean("enabled", value).apply()
    }

    fun key(): String = prefs.getString("key", "") ?: ""

    fun setKey(value: String) {
        prefs.edit().putString("key", value.trim()).apply()
    }

    fun model(): String = prefs.getString("model", DEFAULT_MODEL) ?: DEFAULT_MODEL

    fun setModel(value: String) {
        val v = value.trim()
        prefs.edit().putString("model", if (v.isEmpty()) DEFAULT_MODEL else v).apply()
    }

    fun shareData(): Boolean = prefs.getBoolean("share_data", true)

    fun setShareData(value: Boolean) {
        prefs.edit().putBoolean("share_data", value).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        const val DEFAULT_MODEL = "gemini-3.8-flash"
    }
}

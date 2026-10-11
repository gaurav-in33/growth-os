package com.growthos.personal.reminders

import java.util.Locale

/**
 * Notification rules (fixed for now, configurable in a later step):
 * - Quiet hours 10:00 PM - 5:00 AM: no ordinary reminders.
 * - Protected focus block 10:00 AM - 12:00 PM: no ordinary reminders.
 * - The wake-up alarm is the only exception.
 */
object NotificationRules {
    const val QUIET_START = 22 * 60
    const val QUIET_END = 5 * 60
    const val PROTECTED_START = 10 * 60
    const val PROTECTED_END = 12 * 60

    fun reason(minuteOfDay: Int): String? = when {
        minuteOfDay >= QUIET_START || minuteOfDay < QUIET_END -> "Quiet hours"
        minuteOfDay in PROTECTED_START until PROTECTED_END -> "Protected focus block"
        else -> null
    }
}

fun formatMinute(minuteOfDay: Int): String {
    val h24 = minuteOfDay / 60
    val min = minuteOfDay % 60
    val suffix = if (h24 < 12) "AM" else "PM"
    val h12 = if (h24 % 12 == 0) 12 else h24 % 12
    return String.format(Locale.US, "%d:%02d %s", h12, min, suffix)
}

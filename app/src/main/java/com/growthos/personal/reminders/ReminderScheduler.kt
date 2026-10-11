package com.growthos.personal.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.growthos.personal.MainActivity
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.SettingsStore
import java.time.LocalDate
import java.time.ZoneId

object ReminderScheduler {

    const val CH_ROUTINE = "routine_v1"
    const val CH_CONFIRM = "confirm_v1"
    const val CH_WAKE = "wake_alarm_v1"

    const val ACTION_ROUTINE = "com.growthos.personal.ROUTINE"
    const val ACTION_WAKE = "com.growthos.personal.WAKE"
    const val ACTION_SNOOZE = "com.growthos.personal.SNOOZE"
    const val ACTION_AWAKE = "com.growthos.personal.AWAKE"
    const val ACTION_WAKE_DISMISSED = "com.growthos.personal.WAKE_DISMISSED"

    // Snooze is exactly 5 minutes.
    const val SNOOZE_MILLIS = 5 * 60 * 1000L

    private const val CODE_WAKE = 1
    private const val CODE_WAKE_SNOOZE = 2
    private const val CODE_ROUTINE_BASE = 1000
    private const val CODE_ROUTINE_SNOOZE_BASE = 3000

    private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)

        val routine = NotificationChannel(CH_ROUTINE, "Routine reminders", NotificationManager.IMPORTANCE_DEFAULT)
        routine.description = "Reminders for your daily routine"
        nm.createNotificationChannel(routine)

        val confirm = NotificationChannel(CH_CONFIRM, "Wake-up confirmation", NotificationManager.IMPORTANCE_LOW)
        confirm.description = "Silent prompt to confirm that you are awake"
        nm.createNotificationChannel(confirm)

        val wake = NotificationChannel(CH_WAKE, "Wake-up alarm", NotificationManager.IMPORTANCE_HIGH)
        wake.description = "Your wake-up alarm"
        wake.setSound(
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        wake.enableVibration(true)
        nm.createNotificationChannel(wake)
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        val am = context.getSystemService(AlarmManager::class.java)
        return am.canScheduleExactAlarms()
    }

    fun nextTriggerMillis(startMinute: Int, afterMillis: Long): Long {
        val zone = ZoneId.systemDefault()
        var dt = LocalDate.now(zone).atTime(startMinute / 60, startMinute % 60).atZone(zone)
        if (dt.toInstant().toEpochMilli() <= afterMillis) {
            dt = dt.plusDays(1)
        }
        return dt.toInstant().toEpochMilli()
    }

    private fun broadcast(context: Context, code: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(context, code, intent, FLAGS)

    private fun receiverIntent(context: Context, action: String): Intent =
        Intent(context, AlarmReceiver::class.java).setAction(action)

    private fun setExactCompat(context: Context, triggerAt: Long, pi: PendingIntent) {
        val am = context.getSystemService(AlarmManager::class.java)
        try {
            if (canScheduleExact(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    // ---------- routine reminders ----------

    private fun routineIntent(context: Context, id: Long, title: String, minute: Int, snoozed: Boolean): Intent =
        receiverIntent(context, ACTION_ROUTINE)
            .putExtra("id", id)
            .putExtra("title", title)
            .putExtra("minute", minute)
            .putExtra("snoozed", snoozed)

    fun scheduleRoutine(context: Context, id: Long, title: String, startMinute: Int) {
        ensureChannels(context)
        val now = System.currentTimeMillis()
        val triggerAt = nextTriggerMillis(startMinute, now + 60_000L)
        val pi = broadcast(
            context,
            CODE_ROUTINE_BASE + id.toInt(),
            routineIntent(context, id, title, startMinute, false)
        )
        setExactCompat(context, triggerAt, pi)
    }

    fun cancelRoutine(context: Context, id: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(broadcast(context, CODE_ROUTINE_BASE + id.toInt(), receiverIntent(context, ACTION_ROUTINE)))
        am.cancel(broadcast(context, CODE_ROUTINE_SNOOZE_BASE + id.toInt(), receiverIntent(context, ACTION_ROUTINE)))
    }

    fun scheduleRoutineSnooze(context: Context, id: Long, title: String, minute: Int) {
        val pi = broadcast(
            context,
            CODE_ROUTINE_SNOOZE_BASE + id.toInt(),
            routineIntent(context, id, title, minute, true)
        )
        setExactCompat(context, System.currentTimeMillis() + SNOOZE_MILLIS, pi)
    }

    // ---------- wake-up alarm ----------

    private fun wakeIntent(context: Context, snoozed: Boolean): Intent =
        receiverIntent(context, ACTION_WAKE).putExtra("snoozed", snoozed)

    private fun setAlarmClockCompat(context: Context, triggerAt: Long, pi: PendingIntent) {
        val am = context.getSystemService(AlarmManager::class.java)
        val showIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun scheduleWake(context: Context, minute: Int) {
        ensureChannels(context)
        val triggerAt = nextTriggerMillis(minute, System.currentTimeMillis() + 60_000L)
        setAlarmClockCompat(context, triggerAt, broadcast(context, CODE_WAKE, wakeIntent(context, false)))
    }

    fun scheduleWakeSnooze(context: Context) {
        val triggerAt = System.currentTimeMillis() + SNOOZE_MILLIS
        setAlarmClockCompat(context, triggerAt, broadcast(context, CODE_WAKE_SNOOZE, wakeIntent(context, true)))
    }

    fun cancelWake(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(broadcast(context, CODE_WAKE, receiverIntent(context, ACTION_WAKE)))
        am.cancel(broadcast(context, CODE_WAKE_SNOOZE, receiverIntent(context, ACTION_WAKE)))
    }

    fun cancelWakeSnooze(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(broadcast(context, CODE_WAKE_SNOOZE, receiverIntent(context, ACTION_WAKE)))
    }

    // ---------- reschedule everything (app start, reboot, update) ----------

    suspend fun rescheduleAll(context: Context) {
        ensureChannels(context)
        val items = AppDatabase.get(context).routineDao().getAll()
        for (item in items) {
            if (item.remind && NotificationRules.reason(item.startMinute) == null) {
                scheduleRoutine(context, item.id, item.title, item.startMinute)
            } else {
                cancelRoutine(context, item.id)
            }
        }
        val settings = SettingsStore(context)
        if (settings.wakeEnabled()) {
            scheduleWake(context, settings.wakeMinute())
        } else {
            cancelWake(context)
        }
    }
}

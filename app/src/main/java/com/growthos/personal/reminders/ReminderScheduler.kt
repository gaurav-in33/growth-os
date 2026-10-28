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
import com.growthos.personal.data.TaskEntity
import com.growthos.personal.data.TaskRules
import java.time.LocalDate
import java.time.ZoneId

object ReminderScheduler {

    const val CH_ROUTINE = "routine_v2"
    const val CH_CONFIRM = "confirm_v1"
    // Channel used by the alarm service: silent here because the service plays the ringtone itself.
    const val CH_WAKE = "wake_alarm_v2"
    // Old channel with a sound, only used if the alarm service cannot start.
    const val CH_WAKE_FALLBACK = "wake_alarm_v1"
    const val CH_REVIEW = "review_v1"
    const val CH_TASK = "task_v1"

    const val ACTION_ROUTINE = "com.growthos.personal.ROUTINE"
    const val ACTION_WAKE = "com.growthos.personal.WAKE"
    const val ACTION_SNOOZE = "com.growthos.personal.SNOOZE"
    const val ACTION_AWAKE = "com.growthos.personal.AWAKE"
    const val ACTION_WAKE_DISMISSED = "com.growthos.personal.WAKE_DISMISSED"
    const val ACTION_REVIEW = "com.growthos.personal.REVIEW"
    const val ACTION_TASK = "com.growthos.personal.TASK"
    const val ACTION_TASK_DONE = "com.growthos.personal.TASK_DONE"
    const val ACTION_TASK_SNOOZE = "com.growthos.personal.TASK_SNOOZE"

    const val NOTIF_TASK_BASE = 500_000

    // Snooze is exactly 5 minutes.
    const val SNOOZE_MILLIS = 5 * 60 * 1000L

    private const val CODE_WAKE = 1
    private const val CODE_WAKE_SNOOZE = 2
    private const val CODE_TEST_ALARM = 5
    private const val CODE_TEST_REMINDER = 6
    private const val CODE_REVIEW = 4
    private const val CODE_ROUTINE_BASE = 1000
    private const val CODE_ROUTINE_SNOOZE_BASE = 3000
    private const val CODE_TASK_BASE = 100_000
    private const val CODE_TASK_SNOOZE_BASE = 200_000

    private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)

        val routine = NotificationChannel(CH_ROUTINE, "Routine reminders", NotificationManager.IMPORTANCE_HIGH)
        routine.description = "Reminders for your daily routine"
        routine.enableVibration(true)
        routine.setSound(
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        nm.createNotificationChannel(routine)

        val confirm = NotificationChannel(CH_CONFIRM, "Wake-up confirmation", NotificationManager.IMPORTANCE_LOW)
        confirm.description = "Silent prompt to confirm that you are awake"
        nm.createNotificationChannel(confirm)

        val wake = NotificationChannel(CH_WAKE, "Wake-up alarm", NotificationManager.IMPORTANCE_HIGH)
        wake.description = "Shown while your wake-up alarm rings"
        wake.setSound(null, null)
        wake.enableVibration(false)
        wake.lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        nm.createNotificationChannel(wake)

        val wakeFallback = NotificationChannel(CH_WAKE_FALLBACK, "Wake-up alarm (backup)", NotificationManager.IMPORTANCE_HIGH)
        wakeFallback.description = "Backup alarm sound if the alarm screen cannot open"
        wakeFallback.setSound(
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        wakeFallback.enableVibration(true)
        nm.createNotificationChannel(wakeFallback)

        val review = NotificationChannel(CH_REVIEW, "Evening review", NotificationManager.IMPORTANCE_DEFAULT)
        review.description = "A daily nudge to review your day"
        nm.createNotificationChannel(review)

        val task = NotificationChannel(CH_TASK, "Task reminders", NotificationManager.IMPORTANCE_HIGH)
        task.description = "Reminders for tasks with a date and time"
        task.enableVibration(true)
        nm.createNotificationChannel(task)
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

    // ---------- task reminders ----------

    private fun taskIntent(context: Context, id: Long, title: String, snoozed: Boolean): Intent =
        receiverIntent(context, ACTION_TASK)
            .putExtra("id", id)
            .putExtra("title", title)
            .putExtra("snoozed", snoozed)

    /** One reminder at the task's due date and time. Skipped in quiet hours and the 10-12 block. */
    fun scheduleTask(context: Context, task: TaskEntity) {
        val day = task.dueEpochDay ?: return
        val minute = task.dueMinute ?: return
        if (!task.remind || task.isDone) return
        if (NotificationRules.reason(minute) != null) return
        ensureChannels(context)
        val zone = ZoneId.systemDefault()
        val triggerAt = LocalDate.ofEpochDay(day)
            .atTime(minute / 60, minute % 60)
            .atZone(zone).toInstant().toEpochMilli()
        if (triggerAt <= System.currentTimeMillis()) return
        val pi = broadcast(context, CODE_TASK_BASE + task.id.toInt(), taskIntent(context, task.id, task.title, false))
        setExactCompat(context, triggerAt, pi)
    }

    fun cancelTask(context: Context, id: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(broadcast(context, CODE_TASK_BASE + id.toInt(), receiverIntent(context, ACTION_TASK)))
        am.cancel(broadcast(context, CODE_TASK_SNOOZE_BASE + id.toInt(), receiverIntent(context, ACTION_TASK)))
    }

    fun scheduleTaskSnooze(context: Context, id: Long, title: String) {
        val pi = broadcast(context, CODE_TASK_SNOOZE_BASE + id.toInt(), taskIntent(context, id, title, true))
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

    // ---------- tests (Plan > Alarm check) ----------

    fun scheduleTestAlarm(context: Context, seconds: Int) {
        ensureChannels(context)
        val intent = receiverIntent(context, ACTION_WAKE)
            .putExtra("snoozed", true)
            .putExtra("test", true)
        setAlarmClockCompat(
            context,
            System.currentTimeMillis() + seconds * 1000L,
            broadcast(context, CODE_TEST_ALARM, intent)
        )
    }

    fun scheduleTestReminder(context: Context, seconds: Int) {
        ensureChannels(context)
        val intent = routineIntent(context, 9999L, "Test reminder", 0, true).putExtra("test", true)
        setExactCompat(
            context,
            System.currentTimeMillis() + seconds * 1000L,
            broadcast(context, CODE_TEST_REMINDER, intent)
        )
    }

    // ---------- evening review reminder ----------

    fun scheduleReview(context: Context, minute: Int) {
        ensureChannels(context)
        val triggerAt = nextTriggerMillis(minute, System.currentTimeMillis() + 60_000L)
        setExactCompat(context, triggerAt, broadcast(context, CODE_REVIEW, receiverIntent(context, ACTION_REVIEW)))
    }

    fun cancelReview(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(broadcast(context, CODE_REVIEW, receiverIntent(context, ACTION_REVIEW)))
    }

    // ---------- reschedule everything (app start, reboot, update) ----------

    suspend fun rescheduleAll(context: Context) {
        ensureChannels(context)
        TaskRules.rollover(context)
        for (task in AppDatabase.get(context).taskDao().getAll()) {
            TaskRules.syncReminder(context, task)
        }
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
        if (settings.reviewEnabled() && NotificationRules.reason(settings.reviewMinute()) == null) {
            scheduleReview(context, settings.reviewMinute())
        } else {
            cancelReview(context)
        }
    }
}

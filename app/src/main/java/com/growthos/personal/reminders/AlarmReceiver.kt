package com.growthos.personal.reminders

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.growthos.personal.MainActivity
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.SettingsStore
import com.growthos.personal.data.WakeConfirmation
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ReminderScheduler.ACTION_ROUTINE -> handleRoutine(context, intent)
            ReminderScheduler.ACTION_WAKE -> handleWake(context, intent)
            ReminderScheduler.ACTION_SNOOZE -> handleSnooze(context, intent)
            ReminderScheduler.ACTION_AWAKE -> handleAwake(context)
            ReminderScheduler.ACTION_WAKE_DISMISSED -> showConfirm(context)
            ReminderScheduler.ACTION_REVIEW -> handleReview(context)
        }
    }

    private fun flags() = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    private fun openAppIntent(context: Context, code: Int): PendingIntent =
        PendingIntent.getActivity(context, code, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

    // ---------- routine ----------

    private fun handleRoutine(context: Context, intent: Intent) {
        val id = intent.getLongExtra("id", -1L)
        val title = intent.getStringExtra("title") ?: return
        val minute = intent.getIntExtra("minute", 0)
        val snoozed = intent.getBooleanExtra("snoozed", false)
        if (id < 0) return

        if (!snoozed) {
            ReminderScheduler.scheduleRoutine(context, id, title, minute)
        }

        val now = LocalTime.now()
        val nowMinute = now.hour * 60 + now.minute
        if (NotificationRules.reason(nowMinute) != null) return

        val snoozeIntent = Intent(context, AlarmReceiver::class.java)
            .setAction(ReminderScheduler.ACTION_SNOOZE)
            .putExtra("type", "routine")
            .putExtra("id", id)
            .putExtra("title", title)
            .putExtra("minute", minute)
        val snoozePi = PendingIntent.getBroadcast(context, 2000 + id.toInt(), snoozeIntent, flags())

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CH_ROUTINE)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("Time for your routine: " + formatMinute(minute))
            .setContentIntent(openAppIntent(context, 20))
            .setAutoCancel(true)
            .addAction(0, "Snooze 5 min", snoozePi)
            .build()
        NotificationManagerCompat.from(context).notify(2000 + id.toInt(), notification)
    }

    // ---------- wake-up alarm ----------

    private fun handleWake(context: Context, intent: Intent) {
        val settings = SettingsStore(context)
        if (!settings.wakeEnabled()) return
        val snoozed = intent.getBooleanExtra("snoozed", false)
        if (!snoozed) {
            ReminderScheduler.scheduleWake(context, settings.wakeMinute())
        }
        showWake(context)
    }

    private fun showWake(context: Context) {
        ReminderScheduler.ensureChannels(context)

        val awakePi = PendingIntent.getBroadcast(
            context, 11,
            Intent(context, AlarmReceiver::class.java).setAction(ReminderScheduler.ACTION_AWAKE),
            flags()
        )
        val snoozePi = PendingIntent.getBroadcast(
            context, 12,
            Intent(context, AlarmReceiver::class.java)
                .setAction(ReminderScheduler.ACTION_SNOOZE)
                .putExtra("type", "wake"),
            flags()
        )
        val deletePi = PendingIntent.getBroadcast(
            context, 13,
            Intent(context, AlarmReceiver::class.java).setAction(ReminderScheduler.ACTION_WAKE_DISMISSED),
            flags()
        )

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CH_WAKE)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Wake up!")
            .setContentText("Tap I'm awake to confirm. Snooze gives 5 more minutes.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openAppIntent(context, 14))
            .setDeleteIntent(deletePi)
            .addAction(0, "I'm awake", awakePi)
            .addAction(0, "Snooze 5 min", snoozePi)
            .setTimeoutAfter(90_000L)
            .build()
        // Keep the sound repeating until you act, but the 90 second timeout stops it for good.
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        NotificationManagerCompat.from(context).notify(NOTIF_WAKE, notification)
    }

    private fun showConfirm(context: Context) {
        ReminderScheduler.ensureChannels(context)
        val awakePi = PendingIntent.getBroadcast(
            context, 11,
            Intent(context, AlarmReceiver::class.java).setAction(ReminderScheduler.ACTION_AWAKE),
            flags()
        )
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CH_CONFIRM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Alarm dismissed")
            .setContentText("Are you awake? Tap I'm awake to confirm.")
            .setContentIntent(openAppIntent(context, 15))
            .setAutoCancel(true)
            .addAction(0, "I'm awake", awakePi)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_CONFIRM, notification)
    }

    private fun handleAwake(context: Context) {
        val nm = NotificationManagerCompat.from(context)
        nm.cancel(NOTIF_WAKE)
        nm.cancel(NOTIF_CONFIRM)
        ReminderScheduler.cancelWakeSnooze(context)

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.get(context).wakeDao().upsert(
                    WakeConfirmation(LocalDate.now().toEpochDay(), System.currentTimeMillis())
                )
            } finally {
                pending.finish()
            }
        }
    }

    // ---------- evening review ----------

    private fun handleReview(context: Context) {
        val settings = SettingsStore(context)
        if (!settings.reviewEnabled()) return
        ReminderScheduler.scheduleReview(context, settings.reviewMinute())

        val now = LocalTime.now()
        val nowMinute = now.hour * 60 + now.minute
        if (NotificationRules.reason(nowMinute) != null) return

        ReminderScheduler.ensureChannels(context)
        val openIntent = Intent(context, MainActivity::class.java).putExtra("open_tab", 4)
        val openPi = PendingIntent.getActivity(context, 16, openIntent, flags())
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CH_REVIEW)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Evening review")
            .setContentText("Take a minute to look at your day: score, habits, notes.")
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_REVIEW, notification)
    }

    // ---------- snooze ----------

    private fun handleSnooze(context: Context, intent: Intent) {
        val nm = NotificationManagerCompat.from(context)
        when (intent.getStringExtra("type")) {
            "wake" -> {
                nm.cancel(NOTIF_WAKE)
                nm.cancel(NOTIF_CONFIRM)
                ReminderScheduler.scheduleWakeSnooze(context)
            }
            "routine" -> {
                val id = intent.getLongExtra("id", -1L)
                val title = intent.getStringExtra("title") ?: return
                val minute = intent.getIntExtra("minute", 0)
                if (id < 0) return
                nm.cancel(2000 + id.toInt())
                ReminderScheduler.scheduleRoutineSnooze(context, id, title, minute)
            }
        }
    }

    companion object {
        const val NOTIF_WAKE = 1
        const val NOTIF_CONFIRM = 2
        const val NOTIF_REVIEW = 3
    }
}

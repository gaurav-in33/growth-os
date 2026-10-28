package com.growthos.personal.data

import android.app.NotificationManager
import android.content.Context
import com.growthos.personal.reminders.ReminderScheduler

object ClearTools {

    /** Cancels every alarm/reminder, deletes all data and resets the saved settings and profile. */
    suspend fun clearEverything(context: Context) {
        val db = AppDatabase.get(context)
        for (task in db.taskDao().getAll()) {
            ReminderScheduler.cancelTask(context, task.id)
        }
        for (item in db.routineDao().getAll()) {
            ReminderScheduler.cancelRoutine(context, item.id)
        }
        ReminderScheduler.cancelWake(context)
        ReminderScheduler.cancelWakeSnooze(context)
        ReminderScheduler.cancelReview(context)
        db.clearAllTables()
        SettingsStore(context).clearAll()
        ProfileStore(context).clearAll()
        AiStore(context).clearAll()
        try {
            context.getSystemService(NotificationManager::class.java).cancelAll()
        } catch (e: Exception) {
        }
    }
}

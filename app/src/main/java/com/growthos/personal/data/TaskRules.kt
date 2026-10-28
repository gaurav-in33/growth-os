package com.growthos.personal.data

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.growthos.personal.reminders.ReminderScheduler
import com.growthos.personal.reminders.formatMinute
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object TaskRules {

    fun today(): Long = LocalDate.now().toEpochDay()

    fun isOverdue(t: TaskEntity, today: Long): Boolean {
        val d = t.dueEpochDay
        return !t.isDone && d != null && d < today
    }

    /** Today tasks = done today + not done and (no due date or due today or earlier). */
    fun todayTasks(all: List<TaskEntity>, today: Long): List<TaskEntity> {
        val list = all.filter {
            val d = it.dueEpochDay
            (it.isDone && it.completedEpochDay == today) || (!it.isDone && (d == null || d <= today))
        }
        return list.sortedWith(
            compareBy<TaskEntity>(
                { it.isDone },
                { if (isOverdue(it, today)) 0 else 1 },
                { it.priority },
                { it.dueEpochDay ?: Long.MAX_VALUE },
                { it.dueMinute ?: 1440 }
            )
        )
    }

    /** Short reason why a task is on the Today list, or null. */
    fun reason(t: TaskEntity, today: Long): String? {
        if (t.isDone) return null
        val d = t.dueEpochDay
        val m = t.dueMinute
        return when {
            isOverdue(t, today) -> "Overdue"
            d == today && m != null -> "Due " + formatMinute(m)
            t.priority == 1 -> "High priority"
            d == today -> "Due today"
            else -> null
        }
    }

    fun dayLabel(epochDay: Long, today: Long): String = when (epochDay) {
        today -> "Today"
        today + 1 -> "Tomorrow"
        today - 1 -> "Yesterday"
        else -> LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
    }

    fun metaLine(t: TaskEntity, today: Long): String {
        val parts = mutableListOf<String>()
        val d = t.dueEpochDay
        val m = t.dueMinute
        if (d != null) {
            var s = dayLabel(d, today)
            if (m != null) s += " " + formatMinute(m)
            parts.add(s)
        }
        if (t.priority == 1) parts.add("High")
        if (t.priority == 3) parts.add("Low")
        if (t.estimateMin > 0) {
            parts.add(if (t.estimateMin >= 60 && t.estimateMin % 60 == 0) "${t.estimateMin / 60} h" else "${t.estimateMin} min")
        }
        if (t.category.isNotEmpty()) parts.add(t.category)
        when (t.energy) {
            1 -> parts.add("Low energy")
            2 -> parts.add("Med energy")
            3 -> parts.add("High energy")
        }
        if (t.repeat == "daily") parts.add("Daily")
        if (t.repeat == "weekly") parts.add("Weekly")
        if (t.remind) parts.add("Reminder")
        return parts.joinToString(" · ")
    }

    // ---------- actions (database + reminders together) ----------

    fun syncReminder(context: Context, t: TaskEntity) {
        ReminderScheduler.cancelTask(context, t.id)
        if (t.remind && !t.isDone) ReminderScheduler.scheduleTask(context, t)
    }

    private fun clearNotification(context: Context, id: Long) {
        NotificationManagerCompat.from(context).cancel(ReminderScheduler.NOTIF_TASK_BASE + id.toInt())
    }

    suspend fun save(context: Context, task: TaskEntity) {
        val dao = AppDatabase.get(context).taskDao()
        val id = if (task.id == 0L) dao.insert(task) else {
            dao.update(task)
            task.id
        }
        syncReminder(context, task.copy(id = id))
    }

    suspend fun toggle(context: Context, task: TaskEntity) {
        val dao = AppDatabase.get(context).taskDao()
        if (task.isDone) {
            dao.setDone(task.id, false, null)
            syncReminder(context, task.copy(isDone = false, completedEpochDay = null))
        } else {
            dao.setDone(task.id, true, today())
            ReminderScheduler.cancelTask(context, task.id)
            clearNotification(context, task.id)
        }
    }

    /** Used by the notification button. */
    suspend fun complete(context: Context, id: Long) {
        val dao = AppDatabase.get(context).taskDao()
        val t = dao.get(id) ?: return
        if (t.isDone) return
        dao.setDone(id, true, today())
        ReminderScheduler.cancelTask(context, id)
        clearNotification(context, id)
    }

    /** Postpone = due + 1 day (an overdue task moves to tomorrow). */
    suspend fun postpone(context: Context, task: TaskEntity) {
        val dao = AppDatabase.get(context).taskDao()
        val base = maxOf(task.dueEpochDay ?: today(), today())
        val updated = task.copy(dueEpochDay = base + 1)
        dao.update(updated)
        syncReminder(context, updated)
    }

    suspend fun delete(context: Context, task: TaskEntity) {
        ReminderScheduler.cancelTask(context, task.id)
        clearNotification(context, task.id)
        AppDatabase.get(context).taskDao().delete(task.id)
    }

    /** Done recurring tasks from an earlier day come back as open tasks with the next due date. */
    suspend fun rollover(context: Context) {
        val dao = AppDatabase.get(context).taskDao()
        val today = today()
        for (t in dao.getAll()) {
            val completed = t.completedEpochDay
            if (t.isDone && t.repeat != "none" && completed != null && completed < today) {
                val step = if (t.repeat == "weekly") 7L else 1L
                var next = (t.dueEpochDay ?: completed) + step
                while (next <= completed) next += step
                dao.update(t.copy(isDone = false, dueEpochDay = next, completedEpochDay = null))
            }
        }
    }
}

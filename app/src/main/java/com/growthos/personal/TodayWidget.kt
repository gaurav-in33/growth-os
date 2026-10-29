package com.growthos.personal

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.growthos.personal.data.AppDatabase
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class TodayWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        Thread {
            try {
                for (id in ids) {
                    manager.updateAppWidget(id, build(context))
                }
            } catch (e: Exception) {
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {

        /** Refreshes every Growth OS widget on the home screen. Safe to call from anywhere. */
        fun refresh(context: Context) {
            val app = context.applicationContext
            Thread {
                try {
                    val manager = AppWidgetManager.getInstance(app)
                    val ids = manager.getAppWidgetIds(ComponentName(app, TodayWidget::class.java))
                    if (ids.isNotEmpty()) {
                        val views = build(app)
                        for (id in ids) manager.updateAppWidget(id, views)
                    }
                } catch (e: Exception) {
                }
            }.start()
        }

        private fun build(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_today)
            val today = LocalDate.now().toEpochDay()
            val db = AppDatabase.get(context)

            val due = runBlocking {
                db.taskDao().getAll()
                    .filter { !it.isDone && it.dueEpochDay != null && it.dueEpochDay <= today }
                    .sortedWith(compareBy({ it.dueEpochDay }, { it.priority }))
            }
            val habits = runBlocking { db.habitDao().observeActive().first() }
            val checkins = runBlocking { db.habitDao().observeCheckins().first() }
            val doneToday = habits.count { h -> checkins.any { it.habitId == h.id && it.epochDay == today } }

            views.setTextViewText(R.id.widget_title, "Growth OS · Today")
            views.setTextViewText(
                R.id.widget_summary,
                if (due.isEmpty()) "No tasks due today" else due.size.toString() + " task(s) due"
            )
            val lines = listOf(R.id.widget_task1, R.id.widget_task2, R.id.widget_task3)
            for (i in lines.indices) {
                val t = due.getOrNull(i)
                val overdue = t != null && t.dueEpochDay != null && t.dueEpochDay < today
                views.setTextViewText(
                    lines[i],
                    if (t == null) "" else (if (overdue) "! " else "• ") + t.title
                )
            }
            views.setTextViewText(
                R.id.widget_habits,
                if (habits.isEmpty()) "" else "Habits: " + doneToday + "/" + habits.size + " done"
            )

            val open = Intent(context, MainActivity::class.java)
            open.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            val tap = PendingIntent.getActivity(
                context,
                7001,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, tap)
            return views
        }
    }
}

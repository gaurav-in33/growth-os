package com.growthos.personal

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.widget.RemoteViews
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.TaskEntity
import com.growthos.personal.data.TaskRules
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class WidgetSnapshot(
    val open: List<TaskEntity>,
    val tasksDone: Int,
    val tasksTotal: Int,
    val habitsDone: Int,
    val habitsTotal: Int,
    val routineDone: Int,
    val routineTotal: Int,
    val water: Int,
    val focusMin: Int,
    val today: Long
) {
    val progress: Float
        get() {
            val total = tasksTotal + habitsTotal + routineTotal
            return if (total == 0) 0f else (tasksDone + habitsDone + routineDone).toFloat() / total
        }
}

abstract class BaseTodayWidget : AppWidgetProvider() {

    abstract fun build(context: Context, snap: WidgetSnapshot): RemoteViews

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        Thread {
            try {
                val snap = WidgetData.load(context)
                for (id in ids) manager.updateAppWidget(id, build(context, snap))
            } catch (e: Exception) {
            } finally {
                pending.finish()
            }
        }.start()
    }
}

object WidgetData {

    fun load(context: Context): WidgetSnapshot {
        val db = AppDatabase.get(context)
        val today = LocalDate.now().toEpochDay()
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val all = runBlocking { db.taskDao().getAll() }
        val todayTasks = TaskRules.todayTasks(all, today)
        val open = todayTasks.filter { !it.isDone }
        val habits = runBlocking { db.habitDao().observeActive().first() }
        val checks = runBlocking { db.habitDao().observeCheckins().first() }
        val habitsDone = habits.count { h -> checks.any { it.habitId == h.id && it.epochDay == today } }
        val routine = runBlocking { db.routineDao().observeAll().first() }
        val routineChecked = runBlocking { db.routineDao().observeCheckedIds(today).first() }
        val water = runBlocking { db.waterDao().observe(today).first() } ?: 0
        val focus = runBlocking { db.focusDao().observeMinutesSince(start).first() }
        return WidgetSnapshot(
            open = open,
            tasksDone = todayTasks.size - open.size,
            tasksTotal = todayTasks.size,
            habitsDone = habitsDone,
            habitsTotal = habits.size,
            routineDone = routine.count { it.id in routineChecked },
            routineTotal = routine.size,
            water = water,
            focusMin = focus,
            today = today
        )
    }

    fun ring(progress: Float, sizePx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val stroke = sizePx * 0.12f
        val inset = stroke / 2f + 2f
        val rect = RectF(inset, inset, sizePx - inset, sizePx - inset)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.parseColor("#2E6C63FF")
        canvas.drawArc(rect, 0f, 360f, false, paint)
        if (progress > 0f) {
            paint.color = Color.parseColor("#FF7A2F")
            canvas.drawArc(rect, -90f, 360f * progress.coerceAtMost(1f), false, paint)
        }
        return bmp
    }

    fun line(task: TaskEntity?, today: Long): String {
        if (task == null) return ""
        val due = task.dueEpochDay
        return (if (due != null && due < today) "! " else "\u2022 ") + task.title
    }

    fun openApp(context: Context, views: RemoteViews) {
        val open = Intent(context, MainActivity::class.java)
        open.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val tap = PendingIntent.getActivity(
            context, 7001, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, tap)
    }

    fun percent(snap: WidgetSnapshot): String = (snap.progress * 100).toInt().toString() + "%"
}

/** Medium widget (4x2). Name kept so a widget already on the home screen keeps working. */
class TodayWidget : BaseTodayWidget() {

    override fun build(context: Context, snap: WidgetSnapshot): RemoteViews {
        val v = RemoteViews(context.packageName, R.layout.widget_today)
        v.setImageViewBitmap(R.id.widget_ring, WidgetData.ring(snap.progress, 220))
        v.setTextViewText(R.id.widget_percent, WidgetData.percent(snap))
        v.setTextViewText(
            R.id.widget_title,
            if (snap.open.isEmpty()) "All clear today" else "Today - " + snap.open.size + " left"
        )
        v.setTextViewText(R.id.widget_task1, if (snap.open.isEmpty()) "Nothing due. Enjoy!" else WidgetData.line(snap.open.getOrNull(0), snap.today))
        v.setTextViewText(R.id.widget_task2, WidgetData.line(snap.open.getOrNull(1), snap.today))
        v.setTextViewText(R.id.widget_task3, WidgetData.line(snap.open.getOrNull(2), snap.today))
        v.setTextViewText(
            R.id.widget_habits,
            "Habits " + snap.habitsDone + "/" + snap.habitsTotal + "   Water " + snap.water + "/8"
        )
        WidgetData.openApp(context, v)
        return v
    }

    companion object {
        /** Refreshes every Growth OS widget (small, medium, large). Safe to call from anywhere. */
        fun refresh(context: Context) {
            val app = context.applicationContext
            Thread {
                try {
                    val manager = AppWidgetManager.getInstance(app)
                    val snap = WidgetData.load(app)
                    val providers = listOf<Pair<Class<*>, BaseTodayWidget>>(
                        TodayWidget::class.java to TodayWidget(),
                        TodayWidgetSmall::class.java to TodayWidgetSmall(),
                        TodayWidgetLarge::class.java to TodayWidgetLarge()
                    )
                    for (p in providers) {
                        val ids = manager.getAppWidgetIds(ComponentName(app, p.first))
                        for (id in ids) manager.updateAppWidget(id, p.second.build(app, snap))
                    }
                } catch (e: Exception) {
                }
            }.start()
        }
    }
}

/** Small widget (2x2): progress ring and tasks left. */
class TodayWidgetSmall : BaseTodayWidget() {

    override fun build(context: Context, snap: WidgetSnapshot): RemoteViews {
        val v = RemoteViews(context.packageName, R.layout.widget_small)
        v.setImageViewBitmap(R.id.widget_ring, WidgetData.ring(snap.progress, 200))
        v.setTextViewText(R.id.widget_percent, WidgetData.percent(snap))
        v.setTextViewText(
            R.id.widget_summary,
            if (snap.open.isEmpty()) "All clear" else snap.open.size.toString() + " left"
        )
        v.setTextViewText(R.id.widget_habits, "Habits " + snap.habitsDone + "/" + snap.habitsTotal)
        WidgetData.openApp(context, v)
        return v
    }
}

/** Large widget (4x4): ring, stats and up to 7 tasks. */
class TodayWidgetLarge : BaseTodayWidget() {

    override fun build(context: Context, snap: WidgetSnapshot): RemoteViews {
        val v = RemoteViews(context.packageName, R.layout.widget_large)
        v.setImageViewBitmap(R.id.widget_ring, WidgetData.ring(snap.progress, 240))
        v.setTextViewText(R.id.widget_percent, WidgetData.percent(snap))
        v.setTextViewText(
            R.id.widget_title,
            "Growth OS - " + LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.ENGLISH))
        )
        v.setTextViewText(
            R.id.widget_summary,
            "Tasks " + snap.tasksDone + "/" + snap.tasksTotal + "   Habits " + snap.habitsDone + "/" + snap.habitsTotal
        )
        v.setTextViewText(R.id.widget_habits, "Routine " + snap.routineDone + "/" + snap.routineTotal)
        v.setTextViewText(R.id.widget_extra, "Water " + snap.water + "/8   Focus " + snap.focusMin + " min")
        val ids = listOf(
            R.id.widget_task1, R.id.widget_task2, R.id.widget_task3, R.id.widget_task4,
            R.id.widget_task5, R.id.widget_task6, R.id.widget_task7
        )
        for (i in ids.indices) {
            val text = if (i == 0 && snap.open.isEmpty()) "Nothing due. Enjoy your day!"
            else WidgetData.line(snap.open.getOrNull(i), snap.today)
            v.setTextViewText(ids[i], text)
        }
        WidgetData.openApp(context, v)
        return v
    }
}

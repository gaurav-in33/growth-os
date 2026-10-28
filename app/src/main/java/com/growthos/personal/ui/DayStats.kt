package com.growthos.personal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.SettingsStore
import com.growthos.personal.data.TaskRules
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

class StatPart(val label: String, val earned: Int, val max: Int)

class DayStats(
    val score: Int,
    val parts: List<StatPart>,
    val xp: Int,
    val level: Int,
    val levelFraction: Float,
    val xpToNext: Int,
    val tasksDone: Int,
    val tasksTotal: Int,
    val habitsDone: Int,
    val habitsTotal: Int,
    val focusMin: Int,
    val glasses: Int,
    val routineDone: Int,
    val routineTotal: Int,
    val routinePaused: Boolean
)

private fun points(ratio: Double, max: Int): Int =
    (ratio.coerceIn(0.0, 1.0) * max).roundToInt()

private fun levelFor(xp: Int): Int {
    var level = 1
    while (50 * (level + 1) * level <= xp) level++
    return level
}

/**
 * Today's score and XP, calculated only from what the user recorded.
 * Weights: tasks 20, habits 20, routine 15, focus 15, water 10, study 10, workout 10.
 */
@Composable
fun rememberDayStats(): DayStats {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val today = remember { LocalDate.now().toEpochDay() }
    val startOfDay = remember {
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    val tasksFlow = remember(db) { db.taskDao().observeAll() }
    val habitsFlow = remember(db) { db.habitDao().observeActive() }
    val checkinsFlow = remember(db) { db.habitDao().observeCheckins() }
    val focusTodayFlow = remember(db) { db.focusDao().observeMinutesSince(startOfDay) }
    val focusAllFlow = remember(db) { db.focusDao().observeMinutesSince(0L) }
    val waterFlow = remember(db) { db.waterDao().observe(today) }
    val studyFlow = remember(db) { db.studyDao().observeSince(0L) }
    val workoutFlow = remember(db) { db.workoutDao().observeSince(0L) }
    val routineFlow = remember(db) { db.routineDao().observeAll() }
    val routineCheckFlow = remember(db) { db.routineDao().observeCheckedIds(today) }

    val tasks by tasksFlow.collectAsState(initial = emptyList())
    val habits by habitsFlow.collectAsState(initial = emptyList())
    val checkins by checkinsFlow.collectAsState(initial = emptyList())
    val focusToday by focusTodayFlow.collectAsState(initial = 0)
    val focusAll by focusAllFlow.collectAsState(initial = 0)
    val water by waterFlow.collectAsState(initial = null)
    val studyLogs by studyFlow.collectAsState(initial = emptyList())
    val workoutLogs by workoutFlow.collectAsState(initial = emptyList())
    val routineItems by routineFlow.collectAsState(initial = emptyList())
    val routineChecked by routineCheckFlow.collectAsState(initial = emptyList())
    val routinePaused = SettingsStore(context).pausedDay() == today
    val routineDone = routineItems.count { it.id in routineChecked }

    val todayTasks = TaskRules.todayTasks(tasks, today)
    val todayDone = todayTasks.count { it.isDone }
    val doneTasks = tasks.count { it.isDone }
    val habitsDone = habits.count { h -> checkins.any { it.habitId == h.id && it.epochDay == today } }
    val glasses = water ?: 0
    val studyMin = studyLogs.filter { it.epochDay == today }.sumOf { it.minutes }
    val workoutMin = workoutLogs.filter { it.epochDay == today }.sumOf { it.minutes }

    val parts = mutableListOf<StatPart>()
    if (todayTasks.isNotEmpty()) {
        parts.add(StatPart("Tasks", points(todayDone.toDouble() / todayTasks.size, 20), 20))
    }
    if (habits.isNotEmpty()) {
        parts.add(StatPart("Habits", points(habitsDone.toDouble() / habits.size, 20), 20))
    }
    if (routineItems.isNotEmpty() && !routinePaused) {
        parts.add(StatPart("Routine", points(routineDone.toDouble() / routineItems.size, 15), 15))
    }
    parts.add(StatPart("Focus", points(focusToday / 50.0, 15), 15))
    parts.add(StatPart("Water", points(glasses / 8.0, 10), 10))
    parts.add(StatPart("Study and coding", points(studyMin / 60.0, 10), 10))
    parts.add(StatPart("Workout", points(workoutMin / 20.0, 10), 10))

    val earned = parts.sumOf { it.earned }
    val possible = parts.sumOf { it.max }
    val score = if (possible == 0) 0 else earned * 100 / possible

    val xp = checkins.size * 10 + doneTasks * 5 + focusAll +
        studyLogs.sumOf { it.minutes } + workoutLogs.sumOf { it.minutes }
    val level = levelFor(xp)
    val levelStart = 50 * level * (level - 1)
    val levelEnd = 50 * (level + 1) * level
    val fraction = (xp - levelStart).toFloat() / (levelEnd - levelStart).toFloat()

    return DayStats(
        score = score,
        parts = parts,
        xp = xp,
        level = level,
        levelFraction = fraction.coerceIn(0f, 1f),
        xpToNext = levelEnd - xp,
        tasksDone = todayDone,
        tasksTotal = todayTasks.size,
        habitsDone = habitsDone,
        habitsTotal = habits.size,
        focusMin = focusToday,
        glasses = glasses,
        routineDone = routineDone,
        routineTotal = routineItems.size,
        routinePaused = routinePaused
    )
}

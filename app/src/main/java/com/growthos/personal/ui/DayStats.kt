package com.growthos.personal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.growthos.personal.data.AppDatabase
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
    val glasses: Int
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
 * Same rules as before: tasks 25, habits 25, focus 20, water 10, study 10, workout 10.
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

    val tasks by tasksFlow.collectAsState(initial = emptyList())
    val habits by habitsFlow.collectAsState(initial = emptyList())
    val checkins by checkinsFlow.collectAsState(initial = emptyList())
    val focusToday by focusTodayFlow.collectAsState(initial = 0)
    val focusAll by focusAllFlow.collectAsState(initial = 0)
    val water by waterFlow.collectAsState(initial = null)
    val studyLogs by studyFlow.collectAsState(initial = emptyList())
    val workoutLogs by workoutFlow.collectAsState(initial = emptyList())

    val doneTasks = tasks.count { it.isDone }
    val habitsDone = habits.count { h -> checkins.any { it.habitId == h.id && it.epochDay == today } }
    val glasses = water ?: 0
    val studyMin = studyLogs.filter { it.epochDay == today }.sumOf { it.minutes }
    val workoutMin = workoutLogs.filter { it.epochDay == today }.sumOf { it.minutes }

    val parts = mutableListOf<StatPart>()
    if (tasks.isNotEmpty()) {
        parts.add(StatPart("Tasks", points(doneTasks.toDouble() / tasks.size, 25), 25))
    }
    if (habits.isNotEmpty()) {
        parts.add(StatPart("Habits", points(habitsDone.toDouble() / habits.size, 25), 25))
    }
    parts.add(StatPart("Focus", points(focusToday / 50.0, 20), 20))
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
        tasksDone = doneTasks,
        tasksTotal = tasks.size,
        habitsDone = habitsDone,
        habitsTotal = habits.size,
        focusMin = focusToday,
        glasses = glasses
    )
}

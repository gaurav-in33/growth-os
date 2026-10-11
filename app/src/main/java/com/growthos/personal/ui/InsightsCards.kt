package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

private class ScorePart(val label: String, val earned: Int, val max: Int)

private fun scorePoints(ratio: Double, max: Int): Int =
    (ratio.coerceIn(0.0, 1.0) * max).roundToInt()

private fun levelForXp(xp: Int): Int {
    var level = 1
    while (50 * (level + 1) * level <= xp) level++
    return level
}

@Composable
fun ScoreCard() {
    val c = Growth.colors
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
    val focusMin = (focusToday ?: 0).toInt()
    val glasses = (water ?: 0).toInt()
    val studyMin = studyLogs.filter { it.epochDay == today }.sumOf { it.minutes }
    val workoutMin = workoutLogs.filter { it.epochDay == today }.sumOf { it.minutes }

    val parts = mutableListOf<ScorePart>()
    if (tasks.isNotEmpty()) {
        parts.add(ScorePart("Tasks", scorePoints(doneTasks.toDouble() / tasks.size, 25), 25))
    }
    if (habits.isNotEmpty()) {
        parts.add(ScorePart("Habits", scorePoints(habitsDone.toDouble() / habits.size, 25), 25))
    }
    parts.add(ScorePart("Focus (50 min goal)", scorePoints(focusMin / 50.0, 20), 20))
    parts.add(ScorePart("Water (8 glasses)", scorePoints(glasses / 8.0, 10), 10))
    parts.add(ScorePart("Study and coding (60 min)", scorePoints(studyMin / 60.0, 10), 10))
    parts.add(ScorePart("Workout (20 min)", scorePoints(workoutMin / 20.0, 10), 10))

    val earned = parts.sumOf { it.earned }
    val possible = parts.sumOf { it.max }
    val score = if (possible == 0) 0 else earned * 100 / possible

    val xp = checkins.size * 10 + doneTasks * 5 + (focusAll ?: 0).toInt() +
        studyLogs.sumOf { it.minutes } + workoutLogs.sumOf { it.minutes }
    val level = levelForXp(xp)
    val levelStart = 50 * level * (level - 1)
    val levelEnd = 50 * (level + 1) * level
    val fraction = (xp - levelStart).toFloat() / (levelEnd - levelStart).toFloat()

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Today's score", color = c.textPrimary, fontSize = 18.sp)
            Text(text = "$score / 100", color = c.accent, fontSize = 32.sp)
            parts.forEach { p ->
                Text(
                    text = p.label + ": " + p.earned + " of " + p.max,
                    color = c.textPrimary,
                    fontSize = 14.sp
                )
            }
            Text(
                text = "Calculated only from what you recorded today. " +
                    "Tasks and habits count only if you have added them.",
                color = c.textSecondary,
                fontSize = 12.sp
            )

            Text(text = "Level $level", color = c.textPrimary, fontSize = 18.sp)
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = c.accent,
                trackColor = c.glassBorder
            )
            Text(
                text = "$xp XP total, ${levelEnd - xp} XP to level ${level + 1}",
                color = c.textPrimary,
                fontSize = 14.sp
            )
            Text(
                text = "XP: 10 per habit tick, 5 per done task, 1 per minute of focus, " +
                    "study, coding or workout. It always matches your saved records.",
                color = c.textSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun WeeklyReportCard() {
    val c = Growth.colors
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val today = remember { LocalDate.now().toEpochDay() }
    val weekStart = today - 6
    val weekStartMillis = remember {
        LocalDate.now().minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    val habitsFlow = remember(db) { db.habitDao().observeActive() }
    val checkinsFlow = remember(db) { db.habitDao().observeCheckins() }
    val focusFlow = remember(db) { db.focusDao().observeMinutesSince(weekStartMillis) }
    val studyFlow = remember(db) { db.studyDao().observeSince(weekStart) }
    val workoutFlow = remember(db) { db.workoutDao().observeSince(weekStart) }
    val dayFlow = remember(db) { db.checkinDao().observeBetween(weekStart, today) }
    val financeFlow = remember(db) { db.financeDao().observeBetween(weekStart, today) }

    val habits by habitsFlow.collectAsState(initial = emptyList())
    val checkins by checkinsFlow.collectAsState(initial = emptyList())
    val focusWeek by focusFlow.collectAsState(initial = 0)
    val studyLogs by studyFlow.collectAsState(initial = emptyList())
    val workoutLogs by workoutFlow.collectAsState(initial = emptyList())
    val dayCheckins by dayFlow.collectAsState(initial = emptyList())
    val finance by financeFlow.collectAsState(initial = emptyList())

    val habitTicks = checkins.count { it.epochDay >= weekStart && it.epochDay <= today }
    val habitPossible = habits.size * 7
    val focusMin = (focusWeek ?: 0).toInt()
    val studyMin = studyLogs.filter { it.kind == "Study" }.sumOf { it.minutes }
    val codingMin = studyLogs.filter { it.kind == "Coding" }.sumOf { it.minutes }
    val workoutDays = workoutLogs.map { it.epochDay }.distinct().size
    val workoutMin = workoutLogs.sumOf { it.minutes }

    val sleepDays = dayCheckins.filter { it.sleepMinutes > 0 }
    val sleepText = if (sleepDays.isEmpty()) {
        "not logged"
    } else {
        formatSleepMinutes(sleepDays.sumOf { it.sleepMinutes } / sleepDays.size) +
            " average over " + sleepDays.size + " days"
    }
    val moodDays = dayCheckins.filter { it.mood > 0 }
    val moodText = if (moodDays.isEmpty()) {
        "not logged"
    } else {
        String.format(Locale.ENGLISH, "%.1f", moodDays.sumOf { it.mood }.toDouble() / moodDays.size) +
            " of 5 over " + moodDays.size + " days"
    }
    val energyDays = dayCheckins.filter { it.energy > 0 }
    val energyText = if (energyDays.isEmpty()) {
        "not logged"
    } else {
        String.format(Locale.ENGLISH, "%.1f", energyDays.sumOf { it.energy }.toDouble() / energyDays.size) +
            " of 5 over " + energyDays.size + " days"
    }
    val spent = finance.filter { it.kind == "Expense" }.sumOf { it.amountPaise }
    val income = finance.filter { it.kind == "Income" }.sumOf { it.amountPaise }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Weekly report", color = c.textPrimary, fontSize = 18.sp)
            Text(
                text = "Last 7 days, recorded facts only.",
                color = c.textSecondary,
                fontSize = 12.sp
            )
            Text(
                text = if (habitPossible == 0) {
                    "Habits: none added yet"
                } else {
                    "Habits: $habitTicks of $habitPossible possible ticks"
                },
                color = c.textPrimary,
                fontSize = 14.sp
            )
            Text(
                text = "Focus: $focusMin min total, about ${focusMin / 7} min per day",
                color = c.textPrimary,
                fontSize = 14.sp
            )
            Text(
                text = "Study: $studyMin min, Coding: $codingMin min",
                color = c.textPrimary,
                fontSize = 14.sp
            )
            Text(
                text = "Workout: $workoutDays active days, $workoutMin min",
                color = c.textPrimary,
                fontSize = 14.sp
            )
            Text(text = "Sleep: $sleepText", color = c.textPrimary, fontSize = 14.sp)
            Text(text = "Mood: $moodText", color = c.textPrimary, fontSize = 14.sp)
            Text(text = "Energy: $energyText", color = c.textPrimary, fontSize = 14.sp)
            Text(
                text = "Money: spent " + formatRupees(spent) + ", income " + formatRupees(income),
                color = c.textPrimary,
                fontSize = 14.sp
            )
        }
    }
}

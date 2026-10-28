package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.growthos.personal.data.SettingsStore
import com.growthos.personal.data.TaskRules
import com.growthos.personal.reminders.formatMinute
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun DailyReviewCard() {
    val c = Growth.colors
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val settings = remember { SettingsStore(context) }
    val today = remember { LocalDate.now().toEpochDay() }
    val startOfDay = remember {
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    val tasksFlow = remember(db) { db.taskDao().observeAll() }
    val habitsFlow = remember(db) { db.habitDao().observeActive() }
    val checkinsFlow = remember(db) { db.habitDao().observeCheckins() }
    val minutesFlow = remember(db) { db.focusDao().observeMinutesSince(startOfDay) }
    val waterFlow = remember(db) { db.waterDao().observe(today) }
    val wakeFlow = remember(db) { db.wakeDao().observe(today) }
    val checkinFlow = remember(db) { db.checkinDao().observe(today) }
    val studyFlow = remember(db) { db.studyDao().observeSince(today) }
    val workoutFlow = remember(db) { db.workoutDao().observeSince(today) }
    val financeFlow = remember(db) { db.financeDao().observeBetween(today, today) }
    val routineFlow = remember(db) { db.routineDao().observeAll() }
    val routineCheckFlow = remember(db) { db.routineDao().observeCheckedIds(today) }

    val tasks by tasksFlow.collectAsState(initial = emptyList())
    val habits by habitsFlow.collectAsState(initial = emptyList())
    val checkins by checkinsFlow.collectAsState(initial = emptyList())
    val focusMinutes by minutesFlow.collectAsState(initial = 0)
    val water by waterFlow.collectAsState(initial = null)
    val wake by wakeFlow.collectAsState(initial = null)
    val checkin by checkinFlow.collectAsState(initial = null)
    val studyLogs by studyFlow.collectAsState(initial = emptyList())
    val workoutLogs by workoutFlow.collectAsState(initial = emptyList())
    val financeToday by financeFlow.collectAsState(initial = emptyList())
    val routineItems by routineFlow.collectAsState(initial = emptyList())
    val routineChecked by routineCheckFlow.collectAsState(initial = emptyList())
    val routinePaused = settings.pausedDay() == today

    val todayTasks = TaskRules.todayTasks(tasks, today)
    val openTasks = todayTasks.filter { !it.isDone }
    val overdueCount = openTasks.count { TaskRules.isOverdue(it, today) }
    val habitsDone = habits.count { h -> checkins.any { it.habitId == h.id && it.epochDay == today } }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Daily review", color = c.textPrimary, fontSize = 18.sp)
            Text(
                text = "Recorded facts only.",
                color = c.textSecondary,
                fontSize = 12.sp
            )
            Text(
                text = "Tasks: ${todayTasks.size - openTasks.size} done, ${openTasks.size} still open" +
                    (if (overdueCount > 0) " ($overdueCount overdue)" else ""),
                color = c.textPrimary,
                fontSize = 15.sp
            )
            openTasks.take(3).forEach {
                val why = TaskRules.reason(it, today)
                Text(
                    text = "  - " + it.title + (if (why != null) " ($why)" else ""),
                    color = c.textSecondary,
                    fontSize = 13.sp
                )
            }
            Text(
                text = "Habits: $habitsDone of ${habits.size} ticked today",
                color = c.textPrimary,
                fontSize = 15.sp
            )
            Text(
                text = if (routinePaused) "Routine: paused today"
                else "Routine: ${routineItems.count { it.id in routineChecked }} of ${routineItems.size} ticked",
                color = c.textPrimary,
                fontSize = 15.sp
            )
            Text(text = "Focus: $focusMinutes min completed today", color = c.textPrimary, fontSize = 15.sp)
            Text(text = "Water: ${water ?: 0} glasses", color = c.textPrimary, fontSize = 15.sp)
            val studyMin = studyLogs.filter { it.kind == "Study" }.sumOf { it.minutes }
            val codingMin = studyLogs.filter { it.kind == "Coding" }.sumOf { it.minutes }
            Text(text = "Study: $studyMin min, Coding: $codingMin min", color = c.textPrimary, fontSize = 15.sp)
            Text(text = "Workout: ${workoutLogs.sumOf { it.minutes }} min", color = c.textPrimary, fontSize = 15.sp)
            val moodValue = checkin?.mood ?: 0
            val energyValue = checkin?.energy ?: 0
            val moodText = if (moodValue == 0) "not logged" else "$moodValue of 5"
            val energyText = if (energyValue == 0) "not logged" else "$energyValue of 5"
            Text(text = "Sleep: " + formatSleepMinutes(checkin?.sleepMinutes ?: 0), color = c.textPrimary, fontSize = 15.sp)
            Text(text = "Mood: $moodText, Energy: $energyText", color = c.textPrimary, fontSize = 15.sp)
            val spentToday = financeToday.filter { it.kind == "Expense" }.sumOf { it.amountPaise }
            Text(text = "Spent today: " + formatRupees(spentToday), color = c.textPrimary, fontSize = 15.sp)
            if (settings.wakeEnabled()) {
                val stamp = wake
                val line = if (stamp == null) {
                    "Wake-up: not confirmed yet"
                } else {
                    val z = Instant.ofEpochMilli(stamp).atZone(ZoneId.systemDefault())
                    "Wake-up: confirmed at " + formatMinute(z.hour * 60 + z.minute)
                }
                Text(text = line, color = c.textPrimary, fontSize = 15.sp)
            }
        }
    }
}

package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.SettingsStore
import com.growthos.personal.data.TaskRules
import com.growthos.personal.reminders.formatMinute
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    val tasks by remember(db) { db.taskDao().observeAll() }.collectAsState(initial = emptyList())
    val habits by remember(db) { db.habitDao().observeActive() }.collectAsState(initial = emptyList())
    val checkins by remember(db) { db.habitDao().observeCheckins() }.collectAsState(initial = emptyList())
    val focusMinutes by remember(db) { db.focusDao().observeMinutesSince(startOfDay) }.collectAsState(initial = 0)
    val water by remember(db) { db.waterDao().observe(today) }.collectAsState(initial = null)
    val wake by remember(db) { db.wakeDao().observe(today) }.collectAsState(initial = null)
    val checkin by remember(db) { db.checkinDao().observe(today) }.collectAsState(initial = null)
    val studyLogs by remember(db) { db.studyDao().observeSince(today) }.collectAsState(initial = emptyList())
    val workoutLogs by remember(db) { db.workoutDao().observeSince(today) }.collectAsState(initial = emptyList())
    val financeToday by remember(db) { db.financeDao().observeBetween(today, today) }.collectAsState(initial = emptyList())
    val routineItems by remember(db) { db.routineDao().observeAll() }.collectAsState(initial = emptyList())
    val routineChecked by remember(db) { db.routineDao().observeCheckedIds(today) }.collectAsState(initial = emptyList())
    val routinePaused = settings.pausedDay() == today

    val todayTasks = TaskRules.todayTasks(tasks, today)
    val openTasks = todayTasks.filter { !it.isDone }
    val tasksDone = todayTasks.size - openTasks.size
    val habitsDone = habits.count { h -> checkins.any { it.habitId == h.id && it.epochDay == today } }
    val routineDone = routineItems.count { it.id in routineChecked }
    val studyMin = studyLogs.sumOf { it.minutes }
    val workoutMin = workoutLogs.sumOf { it.minutes }
    val spentToday = financeToday.filter { it.kind == "Expense" }.sumOf { it.amountPaise }
    val mood = checkin?.mood ?: 0
    val energy = checkin?.energy ?: 0
    val sleep = checkin?.sleepMinutes ?: 0

    fun ratio(done: Int, total: Int): Float = if (total <= 0) 0f else done.toFloat() / total

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    text = "Today's snapshot",
                    color = c.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)),
                    color = c.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("\u2705", "Tasks", tasksDone.toString() + "/" + todayTasks.size, Modifier.weight(1f), ratio(tasksDone, todayTasks.size))
                StatTile("\uD83D\uDD01", "Habits", habitsDone.toString() + "/" + habits.size, Modifier.weight(1f), ratio(habitsDone, habits.size))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    "\uD83D\uDDD3\uFE0F", "Routine",
                    if (routinePaused) "Paused" else routineDone.toString() + "/" + routineItems.size,
                    Modifier.weight(1f),
                    if (routinePaused) null else ratio(routineDone, routineItems.size)
                )
                StatTile("\u23F1\uFE0F", "Focus", focusMinutes.toString() + " min", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("\uD83D\uDCA7", "Water", (water ?: 0).toString() + "/8", Modifier.weight(1f), ratio(water ?: 0, 8))
                StatTile("\uD83D\uDCDA", "Study + coding", studyMin.toString() + " min", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("\uD83C\uDFCB\uFE0F", "Workout", workoutMin.toString() + " min", Modifier.weight(1f))
                StatTile("\uD83D\uDE34", "Sleep", if (sleep > 0) formatSleepMinutes(sleep) else "-", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("\uD83D\uDE42", "Mood", if (mood > 0) mood.toString() + "/5" else "-", Modifier.weight(1f))
                StatTile("\u26A1", "Energy", if (energy > 0) energy.toString() + "/5" else "-", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("\uD83D\uDCB8", "Spent today", formatRupees(spentToday), Modifier.weight(1f))
                val stamp = wake
                val wakeText = if (!settings.wakeEnabled()) "Off"
                else if (stamp == null) "Not yet"
                else {
                    val z = Instant.ofEpochMilli(stamp).atZone(ZoneId.systemDefault())
                    formatMinute(z.hour * 60 + z.minute)
                }
                StatTile("\u23F0", "Wake-up", wakeText, Modifier.weight(1f))
            }
            if (openTasks.isNotEmpty()) {
                Text(
                    text = "Still to do",
                    color = c.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                openTasks.take(3).forEach {
                    val why = TaskRules.reason(it, today)
                    Text(
                        text = "\u2022 " + it.title + (if (why != null) "  (" + why + ")" else ""),
                        color = c.textPrimary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

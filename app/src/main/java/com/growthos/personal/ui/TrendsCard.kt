package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
private fun ChartBlock(title: String, headline: String, content: @Composable () -> Unit) {
    val c = Growth.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = c.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = headline,
                color = c.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        content()
    }
}

@Composable
fun WeeklyReportCard() {
    val c = Growth.colors
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val today = remember { LocalDate.now().toEpochDay() }
    var rangeIndex by rememberSaveable { mutableIntStateOf(0) }
    val days = if (rangeIndex == 0) 7 else 30
    val from = today - days + 1
    val fromMillis = remember(days) {
        LocalDate.ofEpochDay(today - days + 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    val tasks by remember(db) { db.taskDao().observeAll() }.collectAsState(initial = emptyList())
    val habitList by remember(db) { db.habitDao().observeActive() }.collectAsState(initial = emptyList())
    val habitChecks by remember(db) { db.habitDao().observeCheckins() }.collectAsState(initial = emptyList())
    val study by remember(db, days) { db.studyDao().observeSince(from) }.collectAsState(initial = emptyList())
    val workouts by remember(db, days) { db.workoutDao().observeSince(from) }.collectAsState(initial = emptyList())
    val checkins by remember(db, days) { db.checkinDao().observeBetween(from, today) }.collectAsState(initial = emptyList())
    val finance by remember(db, days) { db.financeDao().observeBetween(from, today) }.collectAsState(initial = emptyList())
    val waterDays by remember(db, days) { db.waterDao().observeSince(from) }.collectAsState(initial = emptyList())
    val focus by remember(db, days) { db.focusDao().observeSessionsSince(fromMillis) }.collectAsState(initial = emptyList())

    val dayList = (0 until days).map { from + it }
    val labels = dayList.mapIndexed { i, d ->
        val date = LocalDate.ofEpochDay(d)
        if (days == 7) date.dayOfWeek.name.substring(0, 1)
        else if (i % 5 == 0 || i == days - 1) date.dayOfMonth.toString() else ""
    }
    val showValues = days == 7

    fun perDay(f: (Long) -> Float): List<Float> = dayList.map { f(it) }

    val tasksDone = perDay { d -> tasks.count { it.isDone && it.completedEpochDay == d }.toFloat() }
    val habitsDone = perDay { d -> habitChecks.count { it.epochDay == d }.toFloat() }
    val focusMin = perDay { d ->
        focus.filter {
            Instant.ofEpochMilli(it.endedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay() == d
        }.sumOf { it.minutes }.toFloat()
    }
    val studyMin = perDay { d -> study.filter { it.epochDay == d }.sumOf { it.minutes }.toFloat() }
    val workoutMin = perDay { d -> workouts.filter { it.epochDay == d }.sumOf { it.minutes }.toFloat() }
    val water = perDay { d -> (waterDays.firstOrNull { it.epochDay == d }?.glasses ?: 0).toFloat() }
    val sleepHours = perDay { d -> (checkins.firstOrNull { it.epochDay == d }?.sleepMinutes ?: 0) / 60f }
    val mood = perDay { d -> (checkins.firstOrNull { it.epochDay == d }?.mood ?: 0).toFloat() }
    val energy = perDay { d -> (checkins.firstOrNull { it.epochDay == d }?.energy ?: 0).toFloat() }
    val spent = perDay { d -> finance.filter { it.epochDay == d && it.kind == "Expense" }.sumOf { it.amountPaise } / 100f }
    val received = perDay { d -> finance.filter { it.epochDay == d && it.kind != "Expense" }.sumOf { it.amountPaise } / 100f }

    fun avg(v: List<Float>): String {
        val logged = v.filter { it > 0f }
        return if (logged.isEmpty()) "-" else String.format("%.1f", logged.average())
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Trends",
                    color = c.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniChip(label = "7 days", selected = rangeIndex == 0, onClick = { rangeIndex = 0 })
                    MiniChip(label = "30 days", selected = rangeIndex == 1, onClick = { rangeIndex = 1 })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("\u2705", "Tasks done", tasksDone.sum().toInt().toString(), Modifier.weight(1f))
                StatTile("\u23F1\uFE0F", "Focus", focusMin.sum().toInt().toString() + " min", Modifier.weight(1f))
                StatTile("\uD83D\uDE42", "Avg mood", avg(mood), Modifier.weight(1f))
            }
            ChartBlock("Tasks completed", tasksDone.sum().toInt().toString() + " total") {
                ValueBars(tasksDone, labels, showValues = showValues)
            }
            if (habitList.isNotEmpty()) {
                ChartBlock("Habit ticks", habitsDone.sum().toInt().toString() + " total") {
                    ValueBars(habitsDone, labels, showValues = showValues, color = ChartGreen)
                }
            }
            ChartBlock("Focus time (min)", "avg " + String.format("%.0f", focusMin.average())) {
                ValueBars(focusMin, labels, showValues = showValues, color = ChartPurple)
            }
            ChartBlock("Study and coding (min)", studyMin.sum().toInt().toString() + " total") {
                ValueBars(studyMin, labels, showValues = showValues, color = ChartBlue)
            }
            ChartBlock("Workout (min)", workoutMin.sum().toInt().toString() + " total") {
                ValueBars(workoutMin, labels, showValues = showValues, color = ChartRose)
            }
            ChartBlock("Water (glasses)", "avg " + String.format("%.1f", water.average())) {
                ValueBars(water, labels, showValues = showValues, color = ChartBlue)
            }
            ChartBlock("Sleep (hours)", "avg " + avg(sleepHours)) {
                ValueBars(
                    sleepHours, labels, showValues = showValues, color = ChartPurple,
                    valueText = { String.format("%.1f", it) }
                )
            }
            ChartBlock("Mood and energy", "avg " + avg(mood) + " / " + avg(energy)) {
                TrendLines(
                    series = listOf(mood, energy),
                    colors = listOf(c.accent, ChartPurple),
                    labels = labels,
                    maxValue = 5f
                )
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LegendDotLabel(c.accent, "Mood")
                    LegendDotLabel(ChartPurple, "Energy")
                }
            }
            ChartBlock("Money spent", formatRupees((spent.sum() * 100).toLong())) {
                ValueBars(
                    spent, labels, showValues = showValues, color = ChartRose,
                    valueText = { it.toInt().toString() }
                )
            }
            if (received.any { it > 0f }) {
                ChartBlock("Money received", formatRupees((received.sum() * 100).toLong())) {
                    ValueBars(
                        received, labels, showValues = showValues, color = ChartGreen,
                        valueText = { it.toInt().toString() }
                    )
                }
            }
        }
    }
}

package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
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

@Composable
fun ScoreCard() {
    val c = Growth.colors
    val s = rememberDayStats()

    GlassCard(modifier = Modifier.fillMaxWidth(), glow = true) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RingProgress(progress = s.score / 100f, diameter = 104.dp, stroke = 11.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = s.score.toString(),
                            color = c.textPrimary,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(text = "of 100", color = c.textSecondary, fontSize = 11.sp)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Level ${s.level}",
                        color = c.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    ThinBar(progress = s.levelFraction)
                    Text(
                        text = "${s.xpToNext} XP to level ${s.level + 1}",
                        color = c.textSecondary,
                        fontSize = 12.5.sp
                    )
                }
            }
            s.parts.forEach { p ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row {
                        Text(
                            text = p.label,
                            color = c.textPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = p.earned.toString() + " / " + p.max,
                            color = c.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    ThinBar(progress = if (p.max == 0) 0f else p.earned.toFloat() / p.max)
                }
            }
            Text(
                text = "Score and XP come only from what you recorded.",
                color = c.textTertiary,
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

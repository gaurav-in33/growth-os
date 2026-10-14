package com.growthos.personal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.HabitCheckin
import com.growthos.personal.data.HabitEntity
import com.growthos.personal.data.StreakCalculator
import java.time.LocalDate
import kotlinx.coroutines.launch

private fun streakLabel(days: Int): String =
    if (days == 1) "1 day streak" else "$days day streak"

private fun shortName(name: String): String = name.trim().substringBefore(' ')

/** The habits tick row on the Today screen. */
@Composable
fun TodayHabitsCard() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).habitDao() }
    val habitsFlow = remember(dao) { dao.observeActive() }
    val checkinsFlow = remember(dao) { dao.observeCheckins() }
    val habits by habitsFlow.collectAsState(initial = emptyList())
    val checkins by checkinsFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now().toEpochDay() }
    val doneByHabit = remember(checkins) {
        checkins.groupBy({ it.habitId }, { it.epochDay }).mapValues { it.value.toSet() }
    }
    val doneToday = habits.count { doneByHabit[it.id]?.contains(today) == true }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TileLabel("Habits", trailing = if (habits.isEmpty()) null else "$doneToday/${habits.size}")
            if (habits.isEmpty()) {
                Text(
                    text = "Add habits in the Track tab",
                    color = c.textSecondary,
                    fontSize = 14.sp
                )
            } else {
                habits.chunked(4).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { habit ->
                            val checked = doneByHabit[habit.id]?.contains(today) == true
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (checked) Brush.linearGradient(listOf(c.accent2, c.accent))
                                        else SolidColor(c.track)
                                    )
                                    .clickNoRipple {
                                        scope.launch {
                                            if (checked) dao.deleteCheckin(habit.id, today)
                                            else dao.insertCheckin(HabitCheckin(habit.id, today))
                                        }
                                    }
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = shortName(habit.name),
                                    color = if (checked) Color.White else c.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        repeat(4 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/** Track > Habits. */
@Composable
fun HabitsSection() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).habitDao() }
    val habitsFlow = remember(dao) { dao.observeActive() }
    val checkinsFlow = remember(dao) { dao.observeCheckins() }
    val habits by habitsFlow.collectAsState(initial = emptyList())
    val checkins by checkinsFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now().toEpochDay() }
    val letters = remember { weekLetters() }
    val doneByHabit = remember(checkins) {
        checkins.groupBy({ it.habitId }, { it.epochDay }).mapValues { it.value.toSet() }
    }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<HabitEntity?>(null) }

    val doneToday = habits.count { doneByHabit[it.id]?.contains(today) == true }
    val best = habits.maxOfOrNull { StreakCalculator.streak(doneByHabit[it.id] ?: emptySet(), today) } ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RingProgress(
                        progress = if (habits.isEmpty()) 0f else doneToday.toFloat() / habits.size,
                        diameter = 64.dp,
                        stroke = 7.dp
                    ) {
                        Text(
                            text = "$doneToday/${habits.size}",
                            color = c.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(text = "done today", color = c.textSecondary, fontSize = 13.sp)
                }
            }
            GlassCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Best streak",
                        color = c.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = best.toString(),
                            color = c.textPrimary,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = " days",
                            color = c.textSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                }
            }
        }

        if (habits.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "No habits yet", color = c.textSecondary, fontSize = 15.sp)
            }
        }

        habits.forEach { habit ->
            val done = doneByHabit[habit.id] ?: emptySet()
            val checked = today in done
            val streak = StreakCalculator.streak(done, today)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = habit.name,
                                color = c.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = streakLabel(streak),
                                color = c.textSecondary,
                                fontSize = 12.5.sp
                            )
                        }
                        CheckDot(
                            checked = checked,
                            onClick = {
                                scope.launch {
                                    if (checked) dao.deleteCheckin(habit.id, today)
                                    else dao.insertCheckin(HabitCheckin(habit.id, today))
                                }
                            }
                        )
                        MoreMenu(
                            listOf(
                                "Edit" to { editing = habit },
                                "Archive" to { scope.launch { dao.archive(habit.id) } }
                            )
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (6 downTo 0).forEachIndexed { index, back ->
                            val day = today - back
                            val on = day in done
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (on) Brush.horizontalGradient(listOf(c.accent, c.accent2))
                                            else SolidColor(c.track)
                                        )
                                )
                                Text(
                                    text = letters.getOrElse(index) { "" },
                                    color = c.textTertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        GlowButton(text = "Add habit", onClick = { showAdd = true })
    }

    if (showAdd) {
        TextEditDialog(
            heading = "New habit",
            initial = "",
            maxLength = 40,
            onSave = { name ->
                scope.launch { dao.insertHabit(HabitEntity(name = name, createdEpochDay = today)) }
                showAdd = false
            },
            onDismiss = { showAdd = false },
            extraCheck = { candidate ->
                if (habits.any { it.name.equals(candidate, ignoreCase = true) }) "You already have this habit" else null
            }
        )
    }

    val habitBeingEdited = editing
    if (habitBeingEdited != null) {
        TextEditDialog(
            heading = "Edit habit",
            initial = habitBeingEdited.name,
            maxLength = 40,
            onSave = { newName ->
                scope.launch { dao.rename(habitBeingEdited.id, newName) }
                editing = null
            },
            onDismiss = { editing = null },
            extraCheck = { candidate ->
                if (habits.any { it.id != habitBeingEdited.id && it.name.equals(candidate, ignoreCase = true) }) {
                    "You already have this habit"
                } else {
                    null
                }
            }
        )
    }
}

package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Habits", color = c.textPrimary, fontSize = 18.sp)
            if (habits.isEmpty()) {
                Text(
                    text = "No habits yet. Add your first one in the Track tab.",
                    color = c.textSecondary,
                    fontSize = 14.sp
                )
            } else {
                val doneToday = habits.count { doneByHabit[it.id]?.contains(today) == true }
                Text(
                    text = "$doneToday of ${habits.size} done today",
                    color = c.textSecondary,
                    fontSize = 14.sp
                )
                habits.forEach { habit ->
                    val done = doneByHabit[habit.id] ?: emptySet()
                    val checked = today in done
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { nowChecked ->
                                scope.launch {
                                    if (nowChecked) {
                                        dao.insertCheckin(HabitCheckin(habit.id, today))
                                    } else {
                                        dao.deleteCheckin(habit.id, today)
                                    }
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = c.accent,
                                uncheckedColor = c.textSecondary,
                                checkmarkColor = Color.White
                            )
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = habit.name, color = c.textPrimary, fontSize = 16.sp)
                            Text(
                                text = streakLabel(StreakCalculator.streak(done, today)),
                                color = c.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackScreen() {
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

    var newName by rememberSaveable { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = "Track", color = c.textPrimary, fontSize = 26.sp)

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "New habit", color = c.textPrimary, fontSize = 16.sp)
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        errorText = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g. Morning run") },
                    isError = errorText != null,
                    supportingText = {
                        val message = errorText
                        if (message != null) Text(message)
                    },
                    colors = glassFieldColors()
                )
                Button(
                    onClick = {
                        val name = newName.trim()
                        if (name.isEmpty()) {
                            errorText = "Enter a habit name"
                        } else if (name.length > 40) {
                            errorText = "Keep it under 40 characters"
                        } else if (habits.any { it.name.equals(name, ignoreCase = true) }) {
                            errorText = "You already have this habit"
                        } else {
                            scope.launch {
                                dao.insertHabit(HabitEntity(name = name, createdEpochDay = today))
                            }
                            newName = ""
                            errorText = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.accent,
                        contentColor = Color.White
                    )
                ) {
                    Text("Add habit")
                }
            }
        }

        if (habits.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No habits yet. Add one above and tick it daily on the Today tab.",
                    color = c.textSecondary,
                    fontSize = 15.sp
                )
            }
        }

        habits.forEach { habit ->
            val done = doneByHabit[habit.id] ?: emptySet()
            val last7 = (0..6).count { (today - it) in done }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = habit.name, color = c.textPrimary, fontSize = 17.sp)
                        Text(
                            text = streakLabel(StreakCalculator.streak(done, today)) +
                                "  |  last 7 days: $last7/7",
                            color = c.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                    TextButton(onClick = { scope.launch { dao.archive(habit.id) } }) {
                        Text("Archive", color = c.textSecondary)
                    }
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "How streaks work: a streak counts days in a row. One missed day is " +
                    "forgiven (a recovery day) if you were on track the day before, at most " +
                    "once in any 7 days. Recovery days do not add to the count.",
                color = c.textSecondary,
                fontSize = 13.sp
            )
        }
    }
}

package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.WorkoutLogEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val WORKOUT_TYPES = listOf("Walk", "Gym", "Run", "Other")

/** Track > Fitness. */
@Composable
fun FitnessSection() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).workoutDao() }
    val today = remember { LocalDate.now().toEpochDay() }
    val flow = remember(dao) { dao.observeSince(today - 6) }
    val logs by flow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val letters = remember { weekLetters() }

    var type by rememberSaveable { mutableStateOf("Walk") }
    var minutes by rememberSaveable { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val perDay = (6 downTo 0).map { back ->
        val day = today - back
        logs.filter { it.epochDay == day }.sumOf { it.minutes }
    }
    val activeDays = perDay.count { it > 0 }
    val weekMinutes = perDay.sum()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(label = "Active days", value = "$activeDays", unit = "/ 7", modifier = Modifier.weight(1f))
            StatTile(label = "This week", value = "$weekMinutes", unit = "min", modifier = Modifier.weight(1f))
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TileLabel("Last 7 days")
                BarChart(values = perDay, labels = letters)
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TileLabel("Log a workout")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WORKOUT_TYPES.forEach { t ->
                        MiniChip(label = t, selected = type == t, onClick = { type = t })
                    }
                }
                OutlinedTextField(
                    value = minutes,
                    onValueChange = {
                        minutes = it.filter { ch -> ch.isDigit() }.take(3)
                        errorText = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorText != null,
                    supportingText = {
                        val message = errorText
                        if (message != null) Text(message)
                    },
                    colors = glassFieldColors()
                )
                GlowButton(
                    text = "Log workout",
                    onClick = {
                        val m = minutes.toIntOrNull()
                        if (m == null || m < 1 || m > 600) {
                            errorText = "Enter minutes from 1 to 600"
                        } else {
                            scope.launch {
                                dao.insert(WorkoutLogEntity(epochDay = today, type = type, minutes = m))
                            }
                            minutes = ""
                            errorText = null
                        }
                    }
                )
            }
        }

        if (logs.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TileLabel("Recent")
                    logs.sortedByDescending { it.epochDay }.take(6).forEach { log ->
                        val label = if (log.epochDay == today) "Today" else
                            LocalDate.ofEpochDay(log.epochDay)
                                .format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    color = c.accent,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = log.type,
                                    color = c.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "${log.minutes} min",
                                color = c.textSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            MoreMenu(listOf("Remove" to { scope.launch { dao.delete(log.id) } }))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatTile(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    val c = Growth.colors
    GlassCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                color = c.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = c.textPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = " $unit",
                    color = c.textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }
        }
    }
}

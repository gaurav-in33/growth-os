package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.WorkoutLogEntity
import java.time.LocalDate
import kotlinx.coroutines.launch

private val WORKOUT_TYPES = listOf("Walk", "Gym", "Run", "Other")

@Composable
fun FitnessCard() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).workoutDao() }
    val today = remember { LocalDate.now().toEpochDay() }
    val flow = remember(dao) { dao.observeSince(today - 6) }
    val logs by flow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var type by rememberSaveable { mutableStateOf("Walk") }
    var minutes by rememberSaveable { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val todayLogs = logs.filter { it.epochDay == today }
    val activeDays = logs.map { it.epochDay }.distinct().size
    val weekMinutes = logs.sumOf { it.minutes }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "Fitness", color = c.textPrimary, fontSize = 18.sp)
            Text(
                text = "Last 7 days: $activeDays active days, $weekMinutes min",
                color = c.textSecondary,
                fontSize = 13.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WORKOUT_TYPES.forEach { t ->
                    LogChip(label = t, selected = type == t, onClick = { type = t })
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
            Button(
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
                },
                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White)
            ) {
                Text("Log workout")
            }

            todayLogs.forEach { log ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Today: " + log.type + ", " + log.minutes + " min",
                        color = c.textPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { scope.launch { dao.delete(log.id) } }) {
                        Text("Remove", color = c.textSecondary)
                    }
                }
            }
        }
    }
}

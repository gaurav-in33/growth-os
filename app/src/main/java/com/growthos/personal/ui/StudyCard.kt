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
import com.growthos.personal.data.StudyLogEntity
import java.time.LocalDate
import kotlinx.coroutines.launch

private val STUDY_KINDS = listOf("Study", "Coding")

@Composable
fun StudyCard() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).studyDao() }
    val today = remember { LocalDate.now().toEpochDay() }
    val flow = remember(dao) { dao.observeSince(today - 6) }
    val logs by flow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var kind by rememberSaveable { mutableStateOf("Study") }
    var minutes by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val todayLogs = logs.filter { it.epochDay == today }
    val studyToday = todayLogs.filter { it.kind == "Study" }.sumOf { it.minutes }
    val codingToday = todayLogs.filter { it.kind == "Coding" }.sumOf { it.minutes }
    val weekTotal = logs.sumOf { it.minutes }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "Study and coding", color = c.textPrimary, fontSize = 18.sp)
            Text(
                text = "Today: Study $studyToday min, Coding $codingToday min",
                color = c.textPrimary,
                fontSize = 14.sp
            )
            Text(
                text = "Last 7 days: $weekTotal min total",
                color = c.textSecondary,
                fontSize = 13.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                STUDY_KINDS.forEach { k ->
                    LogChip(label = k, selected = kind == k, onClick = { kind = k })
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
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Topic (optional)") },
                colors = glassFieldColors()
            )
            Button(
                onClick = {
                    val m = minutes.toIntOrNull()
                    val t = topic.trim()
                    if (m == null || m < 1 || m > 600) {
                        errorText = "Enter minutes from 1 to 600"
                    } else if (t.length > 60) {
                        errorText = "Keep the topic under 60 characters"
                    } else {
                        scope.launch {
                            dao.insert(
                                StudyLogEntity(
                                    epochDay = today,
                                    kind = kind,
                                    minutes = m,
                                    topic = t
                                )
                            )
                        }
                        minutes = ""
                        topic = ""
                        errorText = null
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White)
            ) {
                Text("Log session")
            }

            todayLogs.forEach { log ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.kind + ": " + log.minutes + " min" +
                            if (log.topic.isEmpty()) "" else " - " + log.topic,
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

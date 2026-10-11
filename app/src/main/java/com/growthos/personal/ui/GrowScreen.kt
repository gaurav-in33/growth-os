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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.growthos.personal.data.GoalEntity
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val HORIZONS = listOf("Daily", "Weekly", "Long-term")

@Composable
fun GrowScreen() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).goalDao() }
    val goalsFlow = remember(dao) { dao.observeActive() }
    val goals by goalsFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now().toEpochDay() }

    var title by rememberSaveable { mutableStateOf("") }
    var horizon by rememberSaveable { mutableStateOf("Daily") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = "Grow", color = c.textPrimary, fontSize = 26.sp)

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "New goal", color = c.textPrimary, fontSize = 16.sp)
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorText = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g. Finish Kotlin basics") },
                    isError = errorText != null,
                    supportingText = {
                        val message = errorText
                        if (message != null) Text(message)
                    },
                    colors = glassFieldColors()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HORIZONS.forEach { h ->
                        FilterChip(
                            selected = horizon == h,
                            onClick = { horizon = h },
                            label = { Text(h) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = c.textPrimary,
                                selectedContainerColor = c.accent,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Button(
                    onClick = {
                        val trimmed = title.trim()
                        if (trimmed.isEmpty()) {
                            errorText = "Enter a goal title"
                        } else if (trimmed.length > 80) {
                            errorText = "Keep it under 80 characters"
                        } else {
                            scope.launch {
                                dao.insert(
                                    GoalEntity(
                                        title = trimmed,
                                        horizon = horizon,
                                        progress = 0,
                                        createdEpochDay = today
                                    )
                                )
                            }
                            title = ""
                            errorText = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White)
                ) {
                    Text("Add goal")
                }
            }
        }

        if (goals.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No goals yet. Add a daily, weekly or long-term goal above. " +
                        "You set the progress yourself by sliding it.",
                    color = c.textSecondary,
                    fontSize = 15.sp
                )
            }
        }

        HORIZONS.forEach { h ->
            val inGroup = goals.filter { it.horizon == h }
            if (inGroup.isNotEmpty()) {
                Text(text = "$h goals", color = c.textPrimary, fontSize = 18.sp)
                inGroup.forEach { goal ->
                    var draft by remember(goal.id, goal.progress) {
                        mutableFloatStateOf(goal.progress.toFloat())
                    }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = goal.title, color = c.textPrimary, fontSize = 16.sp)
                                    Text(
                                        text = if (draft.roundToInt() >= 100) "Completed" else "${draft.roundToInt()}% done",
                                        color = c.textSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                                TextButton(onClick = { scope.launch { dao.archive(goal.id) } }) {
                                    Text("Archive", color = c.textSecondary)
                                }
                            }
                            Slider(
                                value = draft,
                                onValueChange = { draft = it },
                                onValueChangeFinished = {
                                    scope.launch { dao.setProgress(goal.id, draft.roundToInt()) }
                                },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = c.accent,
                                    activeTrackColor = c.accent,
                                    inactiveTrackColor = c.glassBorder
                                )
                            )
                        }
                    }
                }
            }
        }

        StudyCard()
        FitnessCard()
        ProjectsCard()
    }
}

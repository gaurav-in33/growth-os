package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.GoalEntity
import java.time.LocalDate
import kotlinx.coroutines.launch

private val HORIZONS = listOf("Daily", "Weekly", "Long-term")

@Composable
internal fun StepButton(label: String, onClick: () -> Unit) {
    val c = Growth.colors
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(c.tag)
            .clickNoRipple(onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = c.accent, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** Grow > Goals. */
@Composable
fun GoalsSection() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).goalDao() }
    val flow = remember(dao) { dao.observeActive() }
    val goals by flow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now().toEpochDay() }

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<GoalEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (goals.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "No goals yet", color = c.textSecondary, fontSize = 15.sp)
            }
        }
        goals.forEach { goal ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RingProgress(progress = goal.progress / 100f, diameter = 60.dp, stroke = 6.dp) {
                        Text(
                            text = "${goal.progress}%",
                            color = c.textPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = goal.title,
                            color = c.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (goal.progress >= 100) goal.horizon + " · Completed" else goal.horizon,
                            color = c.textSecondary,
                            fontSize = 12.5.sp
                        )
                    }
                    StepButton("−") {
                        scope.launch { dao.setProgress(goal.id, (goal.progress - 10).coerceAtLeast(0)) }
                    }
                    StepButton("+") {
                        scope.launch { dao.setProgress(goal.id, (goal.progress + 10).coerceAtMost(100)) }
                    }
                    MoreMenu(
                        listOf(
                            "Edit" to { editing = goal },
                            "Archive" to { scope.launch { dao.archive(goal.id) } }
                        )
                    )
                }
            }
        }
        GlowButton(text = "New goal", onClick = { showAdd = true })
    }

    if (showAdd) {
        NewGoalDialog(
            onDismiss = { showAdd = false },
            onSave = { title, horizon ->
                scope.launch {
                    dao.insert(
                        GoalEntity(title = title, horizon = horizon, progress = 0, createdEpochDay = today)
                    )
                }
                showAdd = false
            }
        )
    }

    val goalBeingEdited = editing
    if (goalBeingEdited != null) {
        TextEditDialog(
            heading = "Edit goal",
            initial = goalBeingEdited.title,
            maxLength = 80,
            onSave = { newTitle ->
                scope.launch { dao.rename(goalBeingEdited.id, newTitle) }
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun NewGoalDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    val c = Growth.colors
    var title by remember { mutableStateOf("") }
    var horizon by remember { mutableStateOf("Weekly") }
    var errorText by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.solid,
        titleContentColor = c.textPrimary,
        textContentColor = c.textPrimary,
        title = { Text("New goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorText = null
                    },
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
                        MiniChip(label = h, selected = horizon == h, onClick = { horizon = h })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val t = title.trim()
                if (t.isEmpty()) {
                    errorText = "Enter a goal title"
                } else if (t.length > 80) {
                    errorText = "Keep it under 80 characters"
                } else {
                    onSave(t, horizon)
                }
            }) { Text("Add", color = c.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = c.textSecondary) }
        }
    )
}

package com.growthos.personal.ui

import android.app.DatePickerDialog
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.GoalEntity
import com.growthos.personal.data.GoalMilestoneEntity
import com.growthos.personal.data.GoalRules
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val HORIZONS = listOf("Daily", "Weekly", "Long-term")

private fun goalDeadlineText(daysLeft: Long): String = when {
    daysLeft < 0 -> "Overdue"
    daysLeft == 0L -> "Due today"
    daysLeft == 1L -> "1 day left"
    else -> "$daysLeft days left"
}

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
    val milestoneDao = remember { AppDatabase.get(context).goalMilestoneDao() }
    val flow = remember(dao) { dao.observeActive() }
    val milestoneFlow = remember(milestoneDao) { milestoneDao.observeAll() }
    val goals by flow.collectAsState(initial = emptyList())
    val allMilestones by milestoneFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now().toEpochDay() }

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<GoalEntity?>(null) }
    var addMilestoneFor by remember { mutableStateOf<GoalEntity?>(null) }
    var renamingMilestone by remember { mutableStateOf<GoalMilestoneEntity?>(null) }
    var showHistory by remember { mutableStateOf(false) }

    val active = goals.filter { it.completedEpochDay == null }
    val history = goals.filter { it.completedEpochDay != null }
        .sortedByDescending { it.completedEpochDay ?: 0L }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (active.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "No active goals", color = c.textSecondary, fontSize = 15.sp)
            }
        }
        active.forEach { goal ->
            val mine = allMilestones.filter { it.goalId == goal.id }
            val auto = mine.isNotEmpty()
            val deadline = goal.deadlineEpochDay
            val advice = GoalRules.advice(goal, mine, today)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = goal.title,
                                color = c.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (auto) goal.horizon + " · " + mine.count { it.isDone } + "/" +
                                    mine.size + " milestones" else goal.horizon,
                                color = c.textSecondary,
                                fontSize = 12.5.sp
                            )
                            if (deadline != null) {
                                Row { TagPill(goalDeadlineText(deadline - today)) }
                            }
                        }
                        if (!auto) {
                            StepButton("−") {
                                scope.launch { GoalRules.setProgress(context, goal, goal.progress - 10) }
                            }
                            StepButton("+") {
                                scope.launch { GoalRules.setProgress(context, goal, goal.progress + 10) }
                            }
                        }
                        MoreMenu(
                            listOf(
                                "Edit" to { editing = goal },
                                "Add milestone" to { addMilestoneFor = goal },
                                "Archive" to { scope.launch { dao.archive(goal.id) } }
                            )
                        )
                    }

                    if (advice != null) {
                        Text(
                            text = advice,
                            color = c.accent,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    mine.forEach { ms ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CheckDot(
                                checked = ms.isDone,
                                onClick = { scope.launch { GoalRules.toggleMilestone(context, ms) } }
                            )
                            Text(
                                text = ms.title,
                                color = if (ms.isDone) c.textTertiary else c.textPrimary,
                                fontSize = 15.sp,
                                textDecoration = if (ms.isDone) TextDecoration.LineThrough else null,
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                            )
                            MoreMenu(
                                listOf(
                                    "Rename" to { renamingMilestone = ms },
                                    "Delete" to { scope.launch { GoalRules.deleteMilestone(context, ms) } }
                                )
                            )
                        }
                    }
                    Text(
                        text = "+ Add milestone",
                        color = c.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp).clickNoRipple { addMilestoneFor = goal }
                    )
                }
            }
        }
        GlowButton(text = "New goal", onClick = { showAdd = true })

        if (history.isNotEmpty()) {
            SectionHeader(
                title = "Completed goals",
                trailing = if (showHistory) "Hide" else "Show ${history.size}",
                onTrailing = { showHistory = !showHistory }
            )
            if (showHistory) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        history.forEach { goal ->
                            val done = goal.completedEpochDay ?: today
                            val took = (done - goal.createdEpochDay).coerceAtLeast(0)
                            val doneText = LocalDate.ofEpochDay(done)
                                .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = goal.title,
                                        color = c.textPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = goal.horizon + " · Completed " + doneText + " (" +
                                            took + (if (took == 1L) " day" else " days") + ")",
                                        color = c.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                MoreMenu(listOf("Remove" to { scope.launch { dao.archive(goal.id) } }))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        GoalDialog(
            initial = null,
            onDismiss = { showAdd = false },
            onSave = { title, horizon, deadline ->
                scope.launch {
                    dao.insert(
                        GoalEntity(
                            title = title,
                            horizon = horizon,
                            progress = 0,
                            createdEpochDay = today,
                            deadlineEpochDay = deadline
                        )
                    )
                }
                showAdd = false
            }
        )
    }

    val goalBeingEdited = editing
    if (goalBeingEdited != null) {
        GoalDialog(
            initial = goalBeingEdited,
            onDismiss = { editing = null },
            onSave = { title, horizon, deadline ->
                scope.launch {
                    dao.update(goalBeingEdited.copy(title = title, horizon = horizon, deadlineEpochDay = deadline))
                }
                editing = null
            }
        )
    }

    val milestoneGoal = addMilestoneFor
    if (milestoneGoal != null) {
        TextEditDialog(
            heading = "New milestone",
            initial = "",
            maxLength = 80,
            onSave = { title ->
                scope.launch { GoalRules.addMilestone(context, milestoneGoal.id, title) }
                addMilestoneFor = null
            },
            onDismiss = { addMilestoneFor = null }
        )
    }

    val milestoneBeingRenamed = renamingMilestone
    if (milestoneBeingRenamed != null) {
        TextEditDialog(
            heading = "Rename milestone",
            initial = milestoneBeingRenamed.title,
            maxLength = 80,
            onSave = { title ->
                scope.launch { milestoneDao.rename(milestoneBeingRenamed.id, title) }
                renamingMilestone = null
            },
            onDismiss = { renamingMilestone = null }
        )
    }
}

@Composable
private fun GoalDialog(
    initial: GoalEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, Long?) -> Unit
) {
    val c = Growth.colors
    val context = LocalContext.current
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var horizon by remember { mutableStateOf(initial?.horizon ?: "Weekly") }
    var deadline by remember { mutableStateOf(initial?.deadlineEpochDay) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val day = deadline

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.solid,
        titleContentColor = c.textPrimary,
        textContentColor = c.textPrimary,
        title = { Text(if (initial == null) "New goal" else "Edit goal") },
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniChip(
                        label = if (day == null) "Set deadline" else LocalDate.ofEpochDay(day)
                            .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)),
                        selected = day != null,
                        onClick = {
                            val base = LocalDate.ofEpochDay(day ?: LocalDate.now().toEpochDay())
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> deadline = LocalDate.of(y, m + 1, d).toEpochDay() },
                                base.year,
                                base.monthValue - 1,
                                base.dayOfMonth
                            ).show()
                        }
                    )
                    if (day != null) {
                        Text(
                            text = "Clear",
                            color = c.accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickNoRipple { deadline = null }
                        )
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
                    onSave(t, horizon, deadline)
                }
            }) { Text(if (initial == null) "Add" else "Save", color = c.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = c.textSecondary) }
        }
    )
}

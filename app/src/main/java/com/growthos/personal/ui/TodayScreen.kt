package com.growthos.personal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.R
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.TaskEntity
import com.growthos.personal.data.TaskRules
import com.growthos.personal.reminders.formatMinute
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private fun untilText(minutes: Int): String =
    if (minutes >= 60) "${minutes / 60}h ${String.format(Locale.US, "%02d", minutes % 60)}m" else "${minutes}m"

@Composable
fun TodayScreen(name: String, onOpenPlan: () -> Unit) {
    val c = Growth.colors
    val context = LocalContext.current
    val taskDao = remember { AppDatabase.get(context).taskDao() }
    val tasksFlow = remember(taskDao) { taskDao.observeAll() }
    val allTasks by tasksFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val todayEpoch = remember { LocalDate.now().toEpochDay() }
    val tasks = TaskRules.todayTasks(allTasks, todayEpoch)

    var showAdd by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showAll by rememberSaveable { mutableStateOf(false) }

    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH))
    val doneCount = tasks.count { it.isDone }
    val visibleTasks = if (showAll) tasks else tasks.take(6)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(10.dp, CircleShape, ambientColor = c.glow, spotColor = c.glow)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(c.accent2, c.rose)))
                    .padding(2.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.profile_photo),
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (name.isBlank()) greeting else "$greeting, $name",
                    color = c.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = dateText, color = c.textSecondary, fontSize = 13.sp)
            }
            AddButton(onClick = { showAdd = true })
        }

        WakeCard()

        HeroCard(onOpenPlan = onOpenPlan)

        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FocusCard(modifier = Modifier.weight(1f).fillMaxHeight())
            WaterCard(modifier = Modifier.weight(1f).fillMaxHeight())
        }

        TodayHabitsCard()

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TileLabel("Tasks", trailing = if (tasks.isEmpty()) null else "$doneCount/${tasks.size}")
                if (tasks.isEmpty()) {
                    Text(
                        text = "No tasks yet",
                        color = c.textSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                visibleTasks.forEach { task ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheckDot(
                            checked = task.isDone,
                            onClick = { scope.launch { TaskRules.toggle(context, task) } }
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                                .clickNoRipple { scope.launch { TaskRules.toggle(context, task) } }
                        ) {
                            Text(
                                text = task.title,
                                color = if (task.isDone) c.textTertiary else c.textPrimary,
                                fontSize = 15.5.sp,
                                textDecoration = if (task.isDone) TextDecoration.LineThrough else null
                            )
                            val why = TaskRules.reason(task, todayEpoch)
                            if (why != null) {
                                Text(
                                    text = why,
                                    color = if (why == "Overdue") c.accent else c.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (why == "Overdue") FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                        MoreMenu(
                            listOf(
                                "Edit" to { editingTask = task },
                                "Postpone 1 day" to { scope.launch { TaskRules.postpone(context, task) } },
                                "Delete" to { scope.launch { TaskRules.delete(context, task) } }
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (tasks.size > 6) {
                        Text(
                            text = if (showAll) "Show less" else "Show all ${tasks.size}",
                            color = c.textSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickNoRipple { showAll = !showAll }
                        )
                    } else {
                        Box {}
                    }
                    Text(
                        text = "+ Add task",
                        color = c.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickNoRipple { showAdd = true }
                    )
                }
            }
        }

        CheckinCard(compact = true)
    }

    if (showAdd) {
        TaskEditorDialog(
            initial = null,
            defaultDay = todayEpoch,
            onDismiss = { showAdd = false },
            onSave = { t ->
                scope.launch { TaskRules.save(context, t) }
                showAdd = false
            },
            onDelete = null
        )
    }

    val taskBeingEdited = editingTask
    if (taskBeingEdited != null) {
        TaskEditorDialog(
            initial = taskBeingEdited,
            defaultDay = null,
            onDismiss = { editingTask = null },
            onSave = { t ->
                scope.launch { TaskRules.save(context, t) }
                editingTask = null
            },
            onDelete = {
                scope.launch { TaskRules.delete(context, taskBeingEdited) }
                editingTask = null
            }
        )
    }
}

@Composable
private fun HeroCard(onOpenPlan: () -> Unit) {
    val c = Growth.colors
    val context = LocalContext.current
    val stats = rememberDayStats()
    val routineFlow = remember { AppDatabase.get(context).routineDao().observeAll() }
    val routine by routineFlow.collectAsState(initial = emptyList())
    val nowMin = remember {
        val t = LocalTime.now()
        t.hour * 60 + t.minute
    }
    val next = if (stats.routinePaused) null else routine.firstOrNull { it.startMinute > nowMin }
    val ticked = stats.routineDone

    GlassCard(
        modifier = Modifier.fillMaxWidth().clickNoRipple(onOpenPlan),
        glow = true,
        padding = 18.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RingProgress(progress = stats.score / 100f, diameter = 98.dp, stroke = 10.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stats.score.toString(),
                        color = c.textPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(text = "score", color = c.textSecondary, fontSize = 11.sp)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Next up",
                    color = c.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when {
                        stats.routinePaused -> "Routine paused today"
                        next != null -> next.title
                        routine.isEmpty() -> "Add your routine"
                        else -> "All done for today"
                    },
                    color = c.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (next != null) {
                    Text(
                        text = formatMinute(next.startMinute),
                        color = c.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TagPill("in " + untilText(next.startMinute - nowMin))
                        TagPill("Routine $ticked/${routine.size}")
                    }
                }
            }
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(c.tag),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(16.dp)) {
                    val w = this.size.width
                    val h = this.size.height
                    val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.35f, h * 0.15f)
                        lineTo(w * 0.70f, h * 0.50f)
                        lineTo(w * 0.35f, h * 0.85f)
                    }
                    drawPath(path = path, color = c.accent, style = stroke)
                }
            }
        }
    }
}

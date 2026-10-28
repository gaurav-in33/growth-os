package com.growthos.personal.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.TaskEntity
import com.growthos.personal.data.TaskRules
import com.growthos.personal.reminders.NotificationRules
import com.growthos.personal.reminders.formatMinute
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Tasks tab
// ---------------------------------------------------------------------------

@Composable
fun TasksSection() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).taskDao() }
    val flow = remember(dao) { dao.observeAll() }
    val tasks by flow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val today = remember { TaskRules.today() }

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TaskEntity?>(null) }

    val open = tasks.filter { !it.isDone }
    val byPriority = compareBy<TaskEntity>({ it.priority }, { it.dueMinute ?: 1440 }, { it.createdAtMillis })
    val overdue = open.filter { TaskRules.isOverdue(it, today) }
        .sortedWith(compareBy<TaskEntity>({ it.dueEpochDay ?: 0L }, { it.priority }))
    val dueToday = open.filter { it.dueEpochDay == today }.sortedWith(byPriority)
    val upcoming = open.filter { val d = it.dueEpochDay; d != null && d > today }
        .sortedWith(compareBy<TaskEntity>({ it.dueEpochDay ?: 0L }, { it.priority }, { it.dueMinute ?: 1440 }))
    val noDate = open.filter { it.dueEpochDay == null }.sortedWith(byPriority)
    val completed = tasks.filter { it.isDone }.sortedByDescending { it.completedEpochDay ?: 0L }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${open.size} open",
                color = c.textSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(start = 2.dp)
            )
            AddButton(onClick = { showAdd = true }, diameter = 40.dp)
        }

        if (tasks.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "No tasks yet. Tap + to add one.", color = c.textSecondary, fontSize = 14.sp)
            }
        }

        TaskGroup("Overdue", overdue, today, context, scope, onEdit = { editing = it })
        TaskGroup("Today", dueToday, today, context, scope, onEdit = { editing = it })
        TaskGroup("Upcoming", upcoming, today, context, scope, onEdit = { editing = it })
        TaskGroup("No date", noDate, today, context, scope, onEdit = { editing = it })
        TaskGroup("Completed", completed, today, context, scope, onEdit = { editing = it }, limit = 10)
    }

    if (showAdd) {
        TaskEditorDialog(
            initial = null,
            defaultDay = null,
            onDismiss = { showAdd = false },
            onSave = { t ->
                scope.launch { TaskRules.save(context, t) }
                showAdd = false
            },
            onDelete = null
        )
    }
    val current = editing
    if (current != null) {
        TaskEditorDialog(
            initial = current,
            defaultDay = null,
            onDismiss = { editing = null },
            onSave = { t ->
                scope.launch { TaskRules.save(context, t) }
                editing = null
            },
            onDelete = {
                scope.launch { TaskRules.delete(context, current) }
                editing = null
            }
        )
    }
}

@Composable
private fun TaskGroup(
    title: String,
    list: List<TaskEntity>,
    today: Long,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onEdit: (TaskEntity) -> Unit,
    limit: Int = 100
) {
    var showAll by remember { mutableStateOf(false) }
    val c = Growth.colors
    if (list.isEmpty()) return
    val shown = if (showAll) list else list.take(limit)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            TileLabel(title, trailing = list.size.toString())
            shown.forEach { task ->
                TaskRow(
                    task = task,
                    today = today,
                    onToggle = { scope.launch { TaskRules.toggle(context, task) } },
                    onEdit = { onEdit(task) },
                    onPostpone = { scope.launch { TaskRules.postpone(context, task) } },
                    onDelete = { scope.launch { TaskRules.delete(context, task) } }
                )
            }
            if (list.size > limit) {
                Text(
                    text = if (showAll) "Show less" else "Show all ${list.size}",
                    color = c.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp).clickNoRipple { showAll = !showAll }
                )
            }
        }
    }
}

@Composable
fun TaskRow(
    task: TaskEntity,
    today: Long,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onPostpone: () -> Unit,
    onDelete: () -> Unit
) {
    val c = Growth.colors
    val overdue = TaskRules.isOverdue(task, today)
    val meta = TaskRules.metaLine(task, today)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckDot(checked = task.isDone, onClick = onToggle)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .clickNoRipple(onEdit)
        ) {
            Text(
                text = task.title,
                color = if (task.isDone) c.textTertiary else c.textPrimary,
                fontSize = 15.5.sp,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null
            )
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    color = if (overdue) c.accent else c.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (overdue) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
        val items = mutableListOf<Pair<String, () -> Unit>>()
        items.add("Edit" to onEdit)
        if (!task.isDone) items.add("Postpone 1 day" to onPostpone)
        items.add("Delete" to onDelete)
        MoreMenu(items)
    }
}

// ---------------------------------------------------------------------------
// Task editor
// ---------------------------------------------------------------------------

@Composable
private fun ChipRow(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEachIndexed { i, label ->
            MiniChip(label = label, selected = i == selected, onClick = { onSelect(i) })
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    val c = Growth.colors
    Text(text = text, color = c.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
fun TaskEditorDialog(
    initial: TaskEntity?,
    defaultDay: Long?,
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit,
    onDelete: (() -> Unit)?
) {
    val c = Growth.colors
    val context = LocalContext.current
    val today = remember { TaskRules.today() }

    var title by remember { mutableStateOf(initial?.title ?: "") }
    var dueDay by remember { mutableStateOf(initial?.dueEpochDay ?: defaultDay) }
    var dueMinute by remember { mutableStateOf(initial?.dueMinute) }
    var priority by remember { mutableIntStateOf(initial?.priority ?: 2) }
    var estimate by remember { mutableIntStateOf(initial?.estimateMin ?: 0) }
    var category by remember { mutableStateOf(initial?.category ?: "") }
    var energy by remember { mutableIntStateOf(initial?.energy ?: 0) }
    var repeat by remember { mutableStateOf(initial?.repeat ?: "none") }
    var remind by remember { mutableStateOf(initial?.remind ?: false) }
    var error by remember { mutableStateOf<String?>(null) }

    val estimateValues = listOf(0, 15, 30, 60, 120)
    val repeatValues = listOf("none", "daily", "weekly")
    val day = dueDay
    val minute = dueMinute
    val canRemind = day != null && minute != null
    val dateText = if (day == null) "No date" else TaskRules.dayLabel(day, today)
    val timeText = if (minute == null) "No time" else formatMinute(minute)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.solid,
        titleContentColor = c.textPrimary,
        textContentColor = c.textPrimary,
        title = { Text(if (initial == null) "New task" else "Edit task") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        error = null
                    },
                    singleLine = true,
                    label = { Text("Task") },
                    isError = error != null,
                    supportingText = {
                        val message = error
                        if (message != null) Text(message)
                    },
                    colors = glassFieldColors()
                )

                FieldLabel("Date and time")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniChip(label = dateText, selected = day != null, onClick = {
                        val base = LocalDate.ofEpochDay(day ?: today)
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> dueDay = LocalDate.of(y, m + 1, d).toEpochDay() },
                            base.year,
                            base.monthValue - 1,
                            base.dayOfMonth
                        ).show()
                    })
                    MiniChip(label = timeText, selected = minute != null, onClick = {
                        val m = minute ?: 540
                        TimePickerDialog(
                            context,
                            { _, hour, min ->
                                dueMinute = hour * 60 + min
                                if (dueDay == null) dueDay = today
                            },
                            m / 60,
                            m % 60,
                            false
                        ).show()
                    })
                }
                if (day != null || minute != null) {
                    Text(
                        text = "Clear date and time",
                        color = c.accent,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickNoRipple {
                            dueDay = null
                            dueMinute = null
                            remind = false
                        }
                    )
                }

                FieldLabel("Priority")
                ChipRow(listOf("High", "Medium", "Low"), priority - 1, { priority = it + 1 })

                FieldLabel("Duration")
                ChipRow(
                    listOf("None", "15 min", "30 min", "1 h", "2 h"),
                    estimateValues.indexOf(estimate),
                    { estimate = estimateValues[it] }
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { if (it.length <= 20) category = it },
                    singleLine = true,
                    label = { Text("Category (optional)") },
                    colors = glassFieldColors()
                )

                FieldLabel("Energy needed")
                ChipRow(listOf("Any", "Low", "Medium", "High"), energy, { energy = it })

                FieldLabel("Repeat")
                ChipRow(
                    listOf("None", "Daily", "Weekly"),
                    repeatValues.indexOf(repeat),
                    { repeat = repeatValues[it] }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Reminder", color = c.textPrimary, fontSize = 15.sp)
                        if (!canRemind) {
                            Text(text = "Set date and time first", color = c.textSecondary, fontSize = 12.sp)
                        } else if (remind && minute != null && NotificationRules.reason(minute) != null) {
                            Text(
                                text = "No reminder rings at this time (quiet hours or 10 AM - 12 PM)",
                                color = c.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Switch(
                        checked = remind && canRemind,
                        enabled = canRemind,
                        onCheckedChange = { remind = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = c.accent,
                            checkedThumbColor = Color.White,
                            uncheckedTrackColor = c.track,
                            uncheckedThumbColor = Color.White,
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val t = title.trim()
                if (t.isEmpty()) {
                    error = "Enter a task"
                } else if (t.length > 120) {
                    error = "Keep it under 120 characters"
                } else {
                    // a repeating task needs a date to repeat from
                    val finalDay = if (repeat != "none" && dueDay == null) today else dueDay
                    val finalRemind = remind && finalDay != null && dueMinute != null
                    val base = initial ?: TaskEntity(title = t, createdAtMillis = System.currentTimeMillis())
                    onSave(
                        base.copy(
                            title = t,
                            dueEpochDay = finalDay,
                            dueMinute = dueMinute,
                            priority = priority,
                            estimateMin = estimate,
                            category = category.trim(),
                            energy = energy,
                            repeat = repeat,
                            remind = finalRemind
                        )
                    )
                }
            }) { Text("Save", color = c.accent) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete", color = c.textSecondary) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel", color = c.textSecondary) }
            }
        }
    )
}

// ---------------------------------------------------------------------------
// Calendar tab
// ---------------------------------------------------------------------------

@Composable
fun CalendarSection() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).taskDao() }
    val flow = remember(dao) { dao.observeAll() }
    val tasks by flow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val today = remember { TaskRules.today() }

    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf(today) }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TaskEntity?>(null) }

    val month = YearMonth.now().plusMonths(monthOffset.toLong())
    val firstOffset = month.atDay(1).dayOfWeek.value - 1
    val length = month.lengthOfMonth()
    val rows = (firstOffset + length + 6) / 7
    val byDay = tasks.filter { it.dueEpochDay != null }.groupBy { it.dueEpochDay ?: 0L }
    val dayTasks = (byDay[selected] ?: emptyList()).sortedWith(
        compareBy<TaskEntity>({ it.isDone }, { it.dueMinute ?: 1440 }, { it.priority })
    )
    val dayTitle = LocalDate.ofEpochDay(selected)
        .format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "<",
                        color = c.accent,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            .clickNoRipple { monthOffset -= 1 }
                    )
                    Text(
                        text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)),
                        color = c.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = ">",
                        color = c.accent,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            .clickNoRipple { monthOffset += 1 }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                        Text(
                            text = it,
                            color = c.textTertiary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                for (r in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val dayNumber = r * 7 + col - firstOffset + 1
                            if (dayNumber < 1 || dayNumber > length) {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val epoch = month.atDay(dayNumber).toEpochDay()
                                val list = byDay[epoch]
                                val hasOpen = list != null && list.any { !it.isDone }
                                val hasAny = list != null && list.isNotEmpty()
                                val isSelected = epoch == selected
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) c.tag else Color.Transparent)
                                        .clickNoRipple { selected = epoch },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNumber.toString(),
                                            color = if (epoch == today) c.accent else c.textPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = if (epoch == today || isSelected) FontWeight.ExtraBold else FontWeight.Normal
                                        )
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        !hasAny -> Color.Transparent
                                                        hasOpen -> c.accent
                                                        else -> c.textTertiary
                                                    }
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = dayTitle,
                color = c.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 2.dp)
            )
            AddButton(onClick = { showAdd = true }, diameter = 40.dp)
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (dayTasks.isEmpty()) {
                    Text(text = "No tasks on this day", color = c.textSecondary, fontSize = 14.sp)
                }
                dayTasks.forEach { task ->
                    TaskRow(
                        task = task,
                        today = today,
                        onToggle = { scope.launch { TaskRules.toggle(context, task) } },
                        onEdit = { editing = task },
                        onPostpone = { scope.launch { TaskRules.postpone(context, task) } },
                        onDelete = { scope.launch { TaskRules.delete(context, task) } }
                    )
                }
            }
        }
    }

    if (showAdd) {
        TaskEditorDialog(
            initial = null,
            defaultDay = selected,
            onDismiss = { showAdd = false },
            onSave = { t ->
                scope.launch { TaskRules.save(context, t) }
                showAdd = false
            },
            onDelete = null
        )
    }
    val current = editing
    if (current != null) {
        TaskEditorDialog(
            initial = current,
            defaultDay = null,
            onDismiss = { editing = null },
            onSave = { t ->
                scope.launch { TaskRules.save(context, t) }
                editing = null
            },
            onDelete = {
                scope.launch { TaskRules.delete(context, current) }
                editing = null
            }
        )
    }
}

package com.growthos.personal.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.RoutineCheck
import com.growthos.personal.data.RoutineItemEntity
import com.growthos.personal.data.RoutineTemplate
import com.growthos.personal.data.RoutineTemplates
import com.growthos.personal.data.routineBlock
import java.time.LocalDate
import com.growthos.personal.data.SettingsStore
import com.growthos.personal.reminders.NotificationRules
import com.growthos.personal.reminders.ReminderScheduler
import com.growthos.personal.reminders.formatMinute
import java.time.LocalTime
import kotlinx.coroutines.launch

@Composable
fun RoutineSection() {
    val c = Growth.colors
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    val dao = remember { AppDatabase.get(context).routineDao() }
    val itemsFlow = remember(dao) { dao.observeAll() }
    val items by itemsFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now().toEpochDay() }
    val checkedFlow = remember(dao) { dao.observeCheckedIds(today) }
    val checkedIds by checkedFlow.collectAsState(initial = emptyList())
    var paused by remember { mutableStateOf(settings.pausedDay() == today) }
    var showTemplates by remember { mutableStateOf(false) }
    var confirmReplace by remember { mutableStateOf<RoutineTemplate?>(null) }

    var wakeEnabled by remember { mutableStateOf(settings.wakeEnabled()) }
    var wakeMinute by remember { mutableIntStateOf(settings.wakeMinute()) }
    var notice by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<RoutineItemEntity?>(null) }
    var deletingItem by remember { mutableStateOf<RoutineItemEntity?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var showTips by remember { mutableStateOf(false) }
    var afterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    var exactOk by remember { mutableStateOf(ReminderScheduler.canScheduleExact(context)) }

    val nowMin = remember {
        val t = LocalTime.now()
        t.hour * 60 + t.minute
    }

    LaunchedEffect(Unit) {
        if (!settings.routineSeeded() && dao.count() == 0) {
            dao.insertAll(RoutineTemplates.MY_DAY.map { RoutineItemEntity(title = it.second, startMinute = it.first) })
        }
        settings.setRoutineSeeded()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            afterPermission?.invoke()
        } else {
            notice = "Notifications are blocked. Allow them in Android Settings > Apps > Growth OS."
        }
        afterPermission = null
    }

    fun withNotificationPermission(action: () -> Unit) {
        val needs = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needs) {
            afterPermission = action
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action()
        }
    }

    val sleepMinutes = (wakeMinute + 24 * 60 - 23 * 60) % (24 * 60)
    val sleepHours = sleepMinutes / 60f
    val currentIndex = items.indexOfLast { it.startMinute <= nowMin }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ----- wake-up alarm -----
        GlassCard(modifier = Modifier.fillMaxWidth(), glow = true, padding = 18.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Wake-up",
                            color = c.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatMinute(wakeMinute),
                            color = c.textPrimary,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Change time",
                                color = c.accent,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickNoRipple {
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            wakeMinute = hour * 60 + minute
                                            settings.setWakeMinute(wakeMinute)
                                            if (wakeEnabled) ReminderScheduler.scheduleWake(context, wakeMinute)
                                        },
                                        wakeMinute / 60,
                                        wakeMinute % 60,
                                        false
                                    ).show()
                                }
                            )
                            TagPill("Sleep " + String.format(java.util.Locale.US, "%.1f", sleepHours) + " h")
                        }
                    }
                    Switch(
                        checked = wakeEnabled,
                        onCheckedChange = { on ->
                            if (on) {
                                withNotificationPermission {
                                    wakeEnabled = true
                                    settings.setWakeEnabled(true)
                                    ReminderScheduler.scheduleWake(context, wakeMinute)
                                    exactOk = ReminderScheduler.canScheduleExact(context)
                                }
                            } else {
                                wakeEnabled = false
                                settings.setWakeEnabled(false)
                                ReminderScheduler.cancelWake(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = c.accent,
                            checkedThumbColor = Color.White,
                            uncheckedTrackColor = c.track,
                            uncheckedThumbColor = Color.White,
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }
                if (wakeEnabled && !exactOk && Build.VERSION.SDK_INT >= 31) {
                    Text(
                        text = "Allow exact alarms so it rings on time",
                        color = c.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickNoRipple {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:" + context.packageName)
                                )
                            )
                        }
                    )
                }
                Text(
                    text = if (showTips) "Hide alarm tips" else "Alarm tips",
                    color = c.textSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickNoRipple { showTips = !showTips }
                )
                if (showTips) {
                    Text(
                        text = "After you dismiss the alarm, tap I'm awake. Snooze is exactly 5 minutes " +
                            "and the sound stops by itself after 90 seconds. Battery saver, Do Not Disturb " +
                            "and phone-maker settings can delay alarms, so test it once.",
                        color = c.textSecondary,
                        fontSize = 12.5.sp
                    )
                }
            }
        }

        val note = notice
        if (note != null) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = note, color = c.textSecondary, fontSize = 13.sp)
            }
        }

        AlarmCheckCard()

        DayBarCard(nowMin = nowMin)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Routine",
                color = c.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f).padding(start = 2.dp)
            )
            SoftButton(
                text = "Remind all",
                onClick = {
                    withNotificationPermission {
                        scope.launch {
                            for (item in items) {
                                if (NotificationRules.reason(item.startMinute) == null) {
                                    dao.setRemind(item.id, true)
                                    ReminderScheduler.scheduleRoutine(context, item.id, item.title, item.startMinute)
                                }
                            }
                        }
                    }
                }
            )
            AddButton(onClick = { showAdd = true }, diameter = 46.dp)
        }


        if (paused) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Routine paused today",
                            color = c.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "No routine reminders, not counted in score",
                            color = c.textSecondary,
                            fontSize = 12.5.sp
                        )
                    }
                    SoftButton(text = "Resume", onClick = {
                        paused = false
                        settings.setPausedDay(-1L)
                    })
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            TagPill("${items.count { it.id in checkedIds }}/${items.size} done")
            Text(
                text = "Templates",
                color = c.accent,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickNoRipple { showTemplates = true }
            )
            if (!paused) {
                Text(
                    text = "Pause today",
                    color = c.textSecondary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickNoRipple {
                        paused = true
                        settings.setPausedDay(today)
                    }
                )
            }
        }

        Column {
            items.forEachIndexed { index, item ->
                val block = routineBlock(item.startMinute)
                if (index == 0 || block != routineBlock(items[index - 1].startMinute)) {
                    BlockHeader(
                        label = block,
                        isFirst = index == 0,
                        passed = item.startMinute <= nowMin
                    )
                }
                val ticked = item.id in checkedIds
                val reason = NotificationRules.reason(item.startMinute)
                val status = when {
                    item.startMinute > nowMin -> 2
                    index == currentIndex -> 1
                    else -> 0
                }
                TimelineRow(
                    timeText = formatMinute(item.startMinute),
                    status = status,
                    isFirst = false,
                    isLast = index == items.lastIndex,
                    onClick = {
                        scope.launch {
                            if (ticked) dao.deleteCheck(item.id, today)
                            else dao.insertCheck(RoutineCheck(item.id, today))
                        }
                    }
                ) {
                    CheckDot(
                        checked = ticked,
                        onClick = {
                            scope.launch {
                                if (ticked) dao.deleteCheck(item.id, today)
                                else dao.insertCheck(RoutineCheck(item.id, today))
                            }
                        }
                    )
                    Text(
                        text = item.title,
                        color = if (ticked || status == 0) c.textSecondary else c.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (ticked) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f)
                    )
                    if (item.remind) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Reminder on",
                            tint = c.accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (reason != null) {
                        TagPill(if (reason.startsWith("Protected")) "Protected" else "Quiet")
                    }
                    val menu = mutableListOf<Pair<String, () -> Unit>>()
                    menu.add("Edit" to { editing = item })
                    if (reason == null) {
                        menu.add(
                            (if (item.remind) "Turn reminder off" else "Turn reminder on") to {
                                if (item.remind) {
                                    scope.launch {
                                        dao.setRemind(item.id, false)
                                        ReminderScheduler.cancelRoutine(context, item.id)
                                    }
                                } else {
                                    withNotificationPermission {
                                        scope.launch {
                                            dao.setRemind(item.id, true)
                                            ReminderScheduler.scheduleRoutine(
                                                context, item.id, item.title, item.startMinute
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                    menu.add("Delete" to { deletingItem = item })
                    MoreMenu(menu)
                }
            }
        }
    }

    val toDelete = deletingItem
    if (toDelete != null) {
        ThemedDialog(
            title = "Delete this step?",
            onDismiss = { deletingItem = null },
            content = {
                Text(text = toDelete.title, color = c.textSecondary, fontSize = 15.sp)
            },
            actions = {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { deletingItem = null }) { Text("Cancel", color = c.textSecondary) }
                GlowButton(
                    text = "Delete",
                    onClick = {
                        deletingItem = null
                        scope.launch {
                            ReminderScheduler.cancelRoutine(context, toDelete.id)
                            dao.deleteChecksFor(toDelete.id)
                            dao.delete(toDelete.id)
                        }
                    }
                )
            }
        )
    }

    if (showTemplates) {
        TemplatesDialog(
            onDismiss = { showTemplates = false },
            onAdd = { template ->
                scope.launch {
                    val n = RoutineTemplates.add(context, template)
                    notice = if (n == 0) "Everything from " + template.name + " is already in your routine."
                    else "Added $n items from " + template.name + ". Turn on reminders per item."
                }
                showTemplates = false
            },
            onReplace = { template ->
                confirmReplace = template
                showTemplates = false
            }
        )
    }

    val replaceTarget = confirmReplace
    if (replaceTarget != null) {
        AlertDialog(
            onDismissRequest = { confirmReplace = null },
            containerColor = c.solid,
            titleContentColor = c.textPrimary,
            textContentColor = c.textPrimary,
            title = { Text("Replace routine?") },
            text = {
                Text(
                    "Your current ${items.size} items and their ticks will be deleted and replaced by " +
                        replaceTarget.name + ".",
                    color = c.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { RoutineTemplates.replace(context, replaceTarget) }
                    notice = "Routine replaced with " + replaceTarget.name + ". Turn on reminders per item."
                    confirmReplace = null
                }) { Text("Replace", color = c.accent) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReplace = null }) { Text("Cancel", color = c.textSecondary) }
            }
        )
    }

    if (showAdd) {
        RoutineEditDialog(
            initial = null,
            onDismiss = { showAdd = false },
            onSave = { title, minute ->
                scope.launch { dao.insert(RoutineItemEntity(title = title, startMinute = minute)) }
                showAdd = false
            },
            onDelete = null
        )
    }

    val current = editing
    if (current != null) {
        RoutineEditDialog(
            initial = current,
            onDismiss = { editing = null },
            onSave = { title, minute ->
                scope.launch {
                    val stillEligible = NotificationRules.reason(minute) == null
                    val updated = current.copy(
                        title = title,
                        startMinute = minute,
                        remind = current.remind && stillEligible
                    )
                    dao.update(updated)
                    ReminderScheduler.cancelRoutine(context, current.id)
                    if (updated.remind) {
                        ReminderScheduler.scheduleRoutine(context, updated.id, updated.title, updated.startMinute)
                    }
                }
                editing = null
            },
            onDelete = {
                scope.launch {
                    ReminderScheduler.cancelRoutine(context, current.id)
                    dao.deleteChecksFor(current.id)
                    dao.delete(current.id)
                }
                editing = null
            }
        )
    }
}

@Composable
private fun TimelineRow(
    timeText: String,
    status: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val c = Growth.colors
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Text(
            text = timeText,
            color = c.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(66.dp).padding(top = 17.dp, end = 4.dp)
        )
        Box(
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight()
                .drawBehind {
                    val x = this.size.width / 2f
                    val dotY = 24.dp.toPx()
                    val lineColor = if (status == 2) c.glassBorder else c.accent.copy(alpha = 0.7f)
                    drawLine(
                        color = lineColor,
                        start = Offset(x, if (isFirst) dotY else 0f),
                        end = Offset(x, if (isLast) dotY else this.size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                    if (status == 1) {
                        drawCircle(color = c.tint, radius = 11.dp.toPx(), center = Offset(x, dotY))
                    }
                    drawCircle(
                        color = if (status == 2) c.textTertiary.copy(alpha = 0.5f) else c.accent,
                        radius = 6.dp.toPx(),
                        center = Offset(x, dotY)
                    )
                }
        )
        Box(modifier = Modifier.weight(1f).padding(start = 8.dp, bottom = 10.dp)) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                highlight = status == 1,
                padding = 12.dp,
                cornerRadius = 20.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun DayBarCard(nowMin: Int) {
    val c = Growth.colors
    val startDay = 300
    val endDay = 1380
    fun frac(m: Int): Float = ((m - startDay).toFloat() / (endDay - startDay)).coerceIn(0f, 1f)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Today",
                    color = c.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatMinute(nowMin),
                    color = c.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Canvas(modifier = Modifier.fillMaxWidth().height(26.dp)) {
                val w = this.size.width
                val h = 14.dp.toPx()
                val top = (this.size.height - h) / 2f
                val radius = CornerRadius(h / 2f, h / 2f)
                drawRoundRect(
                    color = c.track,
                    topLeft = Offset(0f, top),
                    size = Size(w, h),
                    cornerRadius = radius
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(c.accent, c.accent2)),
                    topLeft = Offset(0f, top),
                    size = Size((w * frac(nowMin)).coerceAtLeast(h), h),
                    cornerRadius = radius
                )
                val ps = w * frac(NotificationRules.PROTECTED_START)
                val pe = w * frac(NotificationRules.PROTECTED_END)
                drawRect(
                    color = Color.White.copy(alpha = 0.6f),
                    topLeft = Offset(ps, top),
                    size = Size(pe - ps, h)
                )
                val qs = w * frac(NotificationRules.QUIET_START)
                drawRect(
                    color = c.textTertiary.copy(alpha = 0.55f),
                    topLeft = Offset(qs, top),
                    size = Size(w - qs, h)
                )
                val nx = w * frac(nowMin)
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(nx - 2.5.dp.toPx(), top - 4.dp.toPx()),
                    size = Size(5.dp.toPx(), h + 8.dp.toPx()),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
            Row {
                Text(text = "5 AM", color = c.textTertiary, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                Text(text = "11 PM", color = c.textTertiary, fontSize = 11.5.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(color = Color.White.copy(alpha = 0.9f), label = "Protected")
                LegendDot(color = c.textTertiary.copy(alpha = 0.7f), label = "Quiet")
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val c = Growth.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 10.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Text(
            text = label,
            color = c.textSecondary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
fun RoutineEditDialog(
    initial: RoutineItemEntity?,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
    onDelete: (() -> Unit)?
) {
    val c = Growth.colors
    val context = LocalContext.current
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var minute by remember { mutableIntStateOf(initial?.startMinute ?: 540) }
    var errorText by remember { mutableStateOf<String?>(null) }

    ThemedDialog(
        title = if (initial == null) "New routine step" else "Edit routine step",
        onDismiss = onDismiss,
        content = {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    errorText = null
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("What do you do?") },
                isError = errorText != null,
                supportingText = {
                    val message = errorText
                    if (message != null) Text(message)
                },
                colors = glassFieldColors()
            )
            Text(
                text = "Start time",
                color = c.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.tag)
                    .clickNoRipple {
                        android.app.TimePickerDialog(
                            context,
                            { _, h, m -> minute = h * 60 + m },
                            minute / 60,
                            minute % 60,
                            false
                        ).show()
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = formatMinute(minute) + "   tap to change",
                    color = c.accent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        actions = {
            if (onDelete != null) {
                TextButton(onClick = onDelete) { Text("Delete", color = Color(0xFFE5484D)) }
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("Cancel", color = c.textSecondary) }
            GlowButton(
                text = "Save",
                onClick = {
                    val t = title.trim()
                    if (t.isEmpty()) errorText = "Write a name"
                    else if (t.length > 60) errorText = "Keep it under 60 characters"
                    else onSave(t, minute)
                }
            )
        }
    )
}

@Composable
private fun BlockHeader(label: String, isFirst: Boolean, passed: Boolean) {
    val c = Growth.colors
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Spacer(modifier = Modifier.width(66.dp))
        Box(
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight()
                .drawBehind {
                    val x = this.size.width / 2f
                    val lineColor = if (passed) c.accent.copy(alpha = 0.7f) else c.glassBorder
                    drawLine(
                        color = lineColor,
                        start = Offset(x, if (isFirst) this.size.height / 2f else 0f),
                        end = Offset(x, this.size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
        )
        Text(
            text = label,
            color = c.textSecondary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 8.dp, top = 6.dp, bottom = 6.dp)
        )
    }
}

@Composable
private fun TemplatesDialog(
    onDismiss: () -> Unit,
    onAdd: (RoutineTemplate) -> Unit,
    onReplace: (RoutineTemplate) -> Unit
) {
    val c = Growth.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.solid,
        titleContentColor = c.textPrimary,
        textContentColor = c.textPrimary,
        title = { Text("Routine templates") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RoutineTemplates.all.forEach { template ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = template.name + " (" + template.items.size + " items)",
                            color = c.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SoftButton(text = "Add", onClick = { onAdd(template) })
                            SoftButton(text = "Replace", onClick = { onReplace(template) })
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = c.textSecondary) }
        }
    )
}

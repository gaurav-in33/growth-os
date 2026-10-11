package com.growthos.personal.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.RoutineItemEntity
import com.growthos.personal.data.SettingsStore
import com.growthos.personal.reminders.NotificationRules
import com.growthos.personal.reminders.ReminderScheduler
import com.growthos.personal.reminders.formatMinute
import kotlinx.coroutines.launch

private val DEFAULT_ROUTINE = listOf(
    315 to "Get ready and go running",
    420 to "Brush teeth and bring milk",
    450 to "Coding and breakfast (finish before 10:00 AM)",
    600 to "Computer class",
    720 to "Return home, self-improvement time",
    780 to "Lunch and milk",
    810 to "Coding session",
    960 to "Outdoor time or walk",
    1140 to "Coding session",
    1305 to "Dinner (by 10:00 PM)",
    1320 to "Wind-down and prepare for sleep",
    1380 to "Target bedtime"
)

@Composable
fun PlanScreen() {
    val c = Growth.colors
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    val dao = remember { AppDatabase.get(context).routineDao() }
    val itemsFlow = remember(dao) { dao.observeAll() }
    val items by itemsFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var wakeEnabled by remember { mutableStateOf(settings.wakeEnabled()) }
    var wakeMinute by remember { mutableIntStateOf(settings.wakeMinute()) }
    var notice by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<RoutineItemEntity?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var afterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    var exactOk by remember { mutableStateOf(ReminderScheduler.canScheduleExact(context)) }

    LaunchedEffect(Unit) {
        if (!settings.routineSeeded() && dao.count() == 0) {
            dao.insertAll(DEFAULT_ROUTINE.map { RoutineItemEntity(title = it.second, startMinute = it.first) })
        }
        settings.setRoutineSeeded()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            afterPermission?.invoke()
        } else {
            notice = "Notifications are blocked, so reminders cannot appear. " +
                "You can allow them in Android Settings > Apps > Growth OS > Notifications."
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = "Plan", color = c.textPrimary, fontSize = 26.sp)

        // ----- wake-up alarm -----
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Wake-up alarm", color = c.textPrimary, fontSize = 17.sp)
                        Text(
                            text = "Rings at ${formatMinute(wakeMinute)} even during quiet hours",
                            color = c.textSecondary,
                            fontSize = 13.sp
                        )
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
                            checkedThumbColor = Color.White
                        )
                    )
                }
                TextButton(onClick = {
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
                }) {
                    Text("Change wake-up time", color = c.accent)
                }
                if (!exactOk && Build.VERSION.SDK_INT >= 31) {
                    Text(
                        text = "Android needs the 'Alarms & reminders' permission for exact alarms. " +
                            "Without it the alarm can be a few minutes late.",
                        color = c.textSecondary,
                        fontSize = 12.sp
                    )
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:" + context.packageName)
                            )
                        )
                    }) {
                        Text("Allow exact alarms", color = c.accent)
                    }
                }
                Text(
                    text = "After you dismiss the alarm you must tap I'm awake. Snooze is exactly 5 minutes. " +
                        "The alarm sound stops by itself after 90 seconds. Battery saver, Do Not Disturb " +
                        "and phone-maker settings can still delay or silence alarms, so please test it once.",
                    color = c.textSecondary,
                    fontSize = 12.sp
                )
            }
        }

        if (sleepHours < 7f) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Sleep note: with a ${formatMinute(wakeMinute)} wake-up and an 11:00 PM bedtime " +
                        "target, your sleep window is about ${String.format(java.util.Locale.US, "%.1f", sleepHours)} " +
                        "hours. You may want to review it when it suits you. Nothing was changed.",
                    color = c.textSecondary,
                    fontSize = 13.sp
                )
            }
        }

        val note = notice
        if (note != null) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = note, color = c.textSecondary, fontSize = 13.sp)
            }
        }

        // ----- routine -----
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Daily routine",
                color = c.textPrimary,
                fontSize = 20.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = {
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
            }) {
                Text("Remind all", color = c.accent)
            }
        }
        Text(
            text = "Quiet hours are 10 PM to 5 AM. The 10 AM to 12 PM computer class is a protected " +
                "block with no routine reminders. Tap an item to edit it.",
            color = c.textSecondary,
            fontSize = 12.sp
        )

        items.forEach { item ->
            val reason = NotificationRules.reason(item.startMinute)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editing = item }
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = formatMinute(item.startMinute), color = c.accent, fontSize = 13.sp)
                        Text(text = item.title, color = c.textPrimary, fontSize = 16.sp)
                    }
                    if (reason != null) {
                        Text(text = reason, color = c.textSecondary, fontSize = 12.sp)
                    } else {
                        Switch(
                            checked = item.remind,
                            onCheckedChange = { on ->
                                if (on) {
                                    withNotificationPermission {
                                        scope.launch {
                                            dao.setRemind(item.id, true)
                                            ReminderScheduler.scheduleRoutine(
                                                context, item.id, item.title, item.startMinute
                                            )
                                        }
                                    }
                                } else {
                                    scope.launch {
                                        dao.setRemind(item.id, false)
                                        ReminderScheduler.cancelRoutine(context, item.id)
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = c.accent,
                                checkedThumbColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        Button(
            onClick = { showAdd = true },
            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White)
        ) {
            Text("Add routine item")
        }
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
                    dao.delete(current.id)
                }
                editing = null
            }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (c.isDark) Color(0xFF14203A) else Color.White,
        titleContentColor = c.textPrimary,
        textContentColor = c.textPrimary,
        title = { Text(if (initial == null) "Add routine item" else "Edit routine item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorText = null
                    },
                    singleLine = true,
                    label = { Text("Activity name") },
                    isError = errorText != null,
                    supportingText = {
                        val message = errorText
                        if (message != null) Text(message)
                    },
                    colors = glassFieldColors()
                )
                TextButton(onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, min -> minute = hour * 60 + min },
                        minute / 60,
                        minute % 60,
                        false
                    ).show()
                }) {
                    Text("Time: ${formatMinute(minute)} (tap to change)", color = c.accent)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = title.trim()
                if (trimmed.isEmpty()) {
                    errorText = "Enter an activity name"
                } else if (trimmed.length > 60) {
                    errorText = "Keep it under 60 characters"
                } else {
                    onSave(trimmed, minute)
                }
            }) {
                Text("Save", color = c.accent)
            }
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

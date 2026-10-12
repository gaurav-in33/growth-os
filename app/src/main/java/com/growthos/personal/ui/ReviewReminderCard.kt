package com.growthos.personal.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.growthos.personal.data.SettingsStore
import com.growthos.personal.reminders.NotificationRules
import com.growthos.personal.reminders.ReminderScheduler
import com.growthos.personal.reminders.formatMinute

@Composable
fun ReviewReminderCard() {
    val c = Growth.colors
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    var enabled by remember { mutableStateOf(settings.reviewEnabled()) }
    var minute by remember { mutableIntStateOf(settings.reviewMinute()) }
    var notice by remember { mutableStateOf<String?>(null) }
    var afterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            afterPermission?.invoke()
        } else {
            notice = "Notifications are blocked, so the reminder cannot appear. " +
                "You can allow them in Android Settings > Apps > Growth OS > Notifications."
        }
        afterPermission = null
    }

    fun turnOn() {
        enabled = true
        settings.setReviewEnabled(true)
        ReminderScheduler.scheduleReview(context, minute)
        notice = null
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Evening review reminder", color = c.textPrimary, fontSize = 17.sp)
                    Text(
                        text = "Every day at " + formatMinute(minute),
                        color = c.textSecondary,
                        fontSize = 13.sp
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { on ->
                        if (on) {
                            val needs = Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            if (needs) {
                                afterPermission = { turnOn() }
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                turnOn()
                            }
                        } else {
                            enabled = false
                            settings.setReviewEnabled(false)
                            ReminderScheduler.cancelReview(context)
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
                    { _, hour, min ->
                        val picked = hour * 60 + min
                        val reason = NotificationRules.reason(picked)
                        if (reason != null) {
                            notice = "That time falls in: " + reason +
                                ". Reminders are paused then, so pick another time."
                        } else {
                            minute = picked
                            settings.setReviewMinute(picked)
                            if (enabled) ReminderScheduler.scheduleReview(context, picked)
                            notice = null
                        }
                    },
                    minute / 60,
                    minute % 60,
                    false
                ).show()
            }) {
                Text("Change time", color = c.accent)
            }
            val message = notice
            if (message != null) {
                Text(text = message, color = c.textPrimary, fontSize = 13.sp)
            }
            Text(
                text = "Tapping the notification opens the More tab with your score and daily review. " +
                    "Not sent during quiet hours (10 PM to 5 AM) or the focus block (10 AM to 12 PM).",
                color = c.textSecondary,
                fontSize = 12.sp
            )
        }
    }
}

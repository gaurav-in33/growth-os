package com.growthos.personal.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.growthos.personal.reminders.AlarmHealth
import com.growthos.personal.reminders.ReminderScheduler

/** Plan > Alarm check: shows what blocks alarms and lets the user test a real alarm and reminder. */
@Composable
fun AlarmCheckCard() {
    val c = Growth.colors
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    val issues = remember(refresh) { AlarmHealth.issues(context) }

    val activity = context as? ComponentActivity
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TileLabel(
                "Alarm check",
                trailing = if (issues.isEmpty()) "All good" else "${issues.size} to fix"
            )
            issues.forEach { issue ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = issue.text,
                        color = c.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    SoftButton(text = issue.fixLabel, onClick = issue.fix)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlowButton(
                    text = "Test alarm",
                    onClick = {
                        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                            message = "Turn on notifications first."
                        } else {
                            ReminderScheduler.scheduleTestAlarm(context, 10)
                            message = "Alarm rings in 10 seconds. You can lock the screen."
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                SoftButton(
                    text = "Test reminder",
                    onClick = {
                        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                            message = "Turn on notifications first."
                        } else {
                            ReminderScheduler.scheduleTestReminder(context, 10)
                            message = "Reminder comes in 10 seconds."
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            val note = message
            if (note != null) {
                Text(text = note, color = c.textSecondary, fontSize = 13.sp)
            }
            if (AlarmHealth.isXiaomiFamily()) {
                Text(
                    text = "Xiaomi, Redmi, Poco: in App settings turn Autostart ON, set Battery saver to " +
                        "No restrictions, and allow notifications on the lock screen.",
                    color = c.textSecondary,
                    fontSize = 12.5.sp
                )
            }
            SoftButton(text = "Open app settings", onClick = { AlarmHealth.openAppSettings(context) })
        }
    }
}

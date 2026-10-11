package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.SettingsStore
import com.growthos.personal.data.WakeConfirmation
import com.growthos.personal.reminders.formatMinute
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.launch

@Composable
fun WakeCard() {
    val c = Growth.colors
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    if (!settings.wakeEnabled()) return

    val dao = remember { AppDatabase.get(context).wakeDao() }
    val today = remember { LocalDate.now().toEpochDay() }
    val confirmedFlow = remember(dao) { dao.observe(today) }
    val confirmedAt by confirmedFlow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    val now = LocalTime.now()
    val nowMinute = now.hour * 60 + now.minute
    val wakeMinute = settings.wakeMinute()
    val stamp = confirmedAt

    if (stamp == null && nowMinute < wakeMinute) return

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Wake-up", color = c.textPrimary, fontSize = 18.sp)
            if (stamp != null) {
                val z = Instant.ofEpochMilli(stamp).atZone(ZoneId.systemDefault())
                Text(
                    text = "Awake confirmed at " + formatMinute(z.hour * 60 + z.minute),
                    color = c.textSecondary,
                    fontSize = 14.sp
                )
            } else {
                Text(
                    text = "Your alarm time (" + formatMinute(wakeMinute) + ") has passed. Are you up?",
                    color = c.textSecondary,
                    fontSize = 14.sp
                )
                Button(
                    onClick = {
                        scope.launch {
                            dao.upsert(WakeConfirmation(today, System.currentTimeMillis()))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White)
                ) {
                    Text("I'm awake")
                }
            }
        }
    }
}

package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Wake-up",
                    color = c.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (stamp != null) {
                    val z = Instant.ofEpochMilli(stamp).atZone(ZoneId.systemDefault())
                    Text(
                        text = "Awake at " + formatMinute(z.hour * 60 + z.minute),
                        color = c.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Alarm was " + formatMinute(wakeMinute),
                        color = c.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (stamp == null) {
                GlowButton(
                    text = "I'm awake",
                    onClick = {
                        scope.launch {
                            dao.upsert(WakeConfirmation(today, System.currentTimeMillis()))
                        }
                    }
                )
            }
        }
    }
}

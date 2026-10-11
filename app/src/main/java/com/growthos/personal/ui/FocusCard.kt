package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.FocusSessionEntity
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun FocusCard() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).focusDao() }
    val startOfDay = remember {
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val minutesFlow = remember(dao) { dao.observeMinutesSince(startOfDay) }
    val todayMinutes by minutesFlow.collectAsState(initial = 0)

    var selectedMinutes by rememberSaveable { mutableIntStateOf(25) }
    var endAtMillis by rememberSaveable { mutableLongStateOf(0L) }
    var remainingSec by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    val running = endAtMillis > 0L

    LaunchedEffect(endAtMillis) {
        if (endAtMillis > 0L) {
            while (true) {
                val now = System.currentTimeMillis()
                val left = ((endAtMillis - now + 999) / 1000).toInt()
                if (left > 0) {
                    remainingSec = left
                    delay(500)
                    continue
                }
                val lateBy = now - endAtMillis
                if (lateBy > 10_000L) {
                    message = "Timer was interrupted, so this session was not counted."
                } else {
                    dao.insert(FocusSessionEntity(endedAtMillis = now, minutes = selectedMinutes))
                    message = "Session complete. Nice work!"
                }
                endAtMillis = 0L
                remainingSec = 0
                break
            }
        }
    }

    val shownSec = if (running) remainingSec else selectedMinutes * 60

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "Focus timer", color = c.textPrimary, fontSize = 18.sp)
            Text(
                text = String.format(Locale.US, "%02d:%02d", shownSec / 60, shownSec % 60),
                color = c.textPrimary,
                fontSize = 44.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(25, 45, 60).forEach { m ->
                    FilterChip(
                        selected = selectedMinutes == m,
                        onClick = { if (!running) selectedMinutes = m },
                        enabled = !running,
                        label = { Text("$m min") },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = c.textPrimary,
                            selectedContainerColor = c.accent,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Button(
                onClick = {
                    if (running) {
                        endAtMillis = 0L
                        remainingSec = 0
                        message = "Timer stopped. This session was not counted."
                    } else {
                        message = null
                        endAtMillis = System.currentTimeMillis() + selectedMinutes * 60_000L
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.accent,
                    contentColor = Color.White
                )
            ) {
                Text(if (running) "Stop (not counted)" else "Start focus")
            }
            Text(
                text = "Focused today: $todayMinutes min",
                color = c.textSecondary,
                fontSize = 14.sp
            )
            val note = message
            if (note != null) {
                Text(text = note, color = c.textSecondary, fontSize = 13.sp)
            } else {
                Text(
                    text = "Keep the app open while the timer runs. Only completed sessions are counted.",
                    color = c.textSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.FocusSessionEntity
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun FocusCard(modifier: Modifier = Modifier) {
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
                    message = "Timer was interrupted, not counted."
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

    GlassCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TileLabel("Focus")
            Text(
                text = String.format(Locale.US, "%02d:%02d", shownSec / 60, shownSec % 60),
                color = c.textPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(25, 45, 60).forEach { m ->
                    MiniChip(
                        label = m.toString(),
                        selected = selectedMinutes == m,
                        onClick = { if (!running) selectedMinutes = m }
                    )
                }
            }
            if (running) {
                SoftButton(
                    text = "Stop",
                    onClick = {
                        endAtMillis = 0L
                        remainingSec = 0
                        message = "Stopped, not counted."
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                GlowButton(
                    text = "Start",
                    onClick = {
                        message = null
                        endAtMillis = System.currentTimeMillis() + selectedMinutes * 60_000L
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            val note = message
            Text(
                text = when {
                    running -> "Keep the app open"
                    note != null -> note
                    else -> "$todayMinutes min today"
                },
                color = c.textSecondary,
                fontSize = 12.5.sp
            )
        }
    }
}

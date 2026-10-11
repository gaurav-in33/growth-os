package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.DayCheckin
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun CheckinCard() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).checkinDao() }
    val today = remember { LocalDate.now().toEpochDay() }
    val flow = remember(dao) { dao.observe(today) }
    val saved by flow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val current = saved ?: DayCheckin(epochDay = today, mood = 0, energy = 0, sleepMinutes = 0)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "Daily check-in", color = c.textPrimary, fontSize = 18.sp)
            Text(
                text = "Tap a number to log it. Tap again to clear.",
                color = c.textSecondary,
                fontSize = 12.sp
            )

            Text(text = "Mood (1 low, 5 great)", color = c.textPrimary, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { v ->
                    LogChip(
                        label = v.toString(),
                        selected = current.mood == v,
                        onClick = {
                            scope.launch {
                                dao.upsert(current.copy(mood = if (current.mood == v) 0 else v))
                            }
                        }
                    )
                }
            }

            Text(text = "Energy (1 low, 5 high)", color = c.textPrimary, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { v ->
                    LogChip(
                        label = v.toString(),
                        selected = current.energy == v,
                        onClick = {
                            scope.launch {
                                dao.upsert(current.copy(energy = if (current.energy == v) 0 else v))
                            }
                        }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "Sleep", color = c.textPrimary, fontSize = 14.sp)
                TextButton(onClick = {
                    val s = current.sleepMinutes
                    scope.launch {
                        dao.upsert(current.copy(sleepMinutes = if (s <= 30) 0 else s - 30))
                    }
                }) {
                    Text("-30m", color = c.accent)
                }
                Text(
                    text = formatSleepMinutes(current.sleepMinutes),
                    color = c.textPrimary,
                    fontSize = 15.sp
                )
                TextButton(onClick = {
                    val s = current.sleepMinutes
                    scope.launch {
                        dao.upsert(current.copy(sleepMinutes = if (s == 0) 420 else minOf(s + 30, 960)))
                    }
                }) {
                    Text("+30m", color = c.accent)
                }
            }
        }
    }
}

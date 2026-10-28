package com.growthos.personal.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.DayCheckin
import com.growthos.personal.reminders.formatMinute
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun RatingRow(label: String, value: Int, onPick: (Int) -> Unit) {
    val c = Growth.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = c.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { v ->
                val on = value == v
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (on) Brush.linearGradient(listOf(c.accent2, c.accent))
                            else SolidColor(c.track)
                        )
                        .clickNoRipple { onPick(v) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = v.toString(),
                        color = if (on) Color.White else c.textSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/** Mood and energy. When compact is false it also shows the sleep stepper (Track > Health). */
@Composable
fun CheckinCard(compact: Boolean = true) {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).checkinDao() }
    val today = remember { LocalDate.now().toEpochDay() }
    val flow = remember(dao) { dao.observe(today) }
    val saved by flow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val current = saved ?: DayCheckin(epochDay = today, mood = 0, energy = 0, sleepMinutes = 0)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TileLabel(if (compact) "How are you feeling?" else "Today's check-in")
            RatingRow("Mood", current.mood) { v ->
                scope.launch { dao.upsert(current.copy(mood = if (current.mood == v) 0 else v)) }
            }
            RatingRow("Energy", current.energy) { v ->
                scope.launch { dao.upsert(current.copy(energy = if (current.energy == v) 0 else v)) }
            }
            if (!compact) {
                val bed = current.bedtimeMinute
                val wake = current.wakeMinute
                fun minutesBetween(b: Int, w: Int, fallback: Int): Int =
                    if (b >= 0 && w >= 0) (w - b + 1440) % 1440 else fallback
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Sleep",
                        color = c.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TimeButton(
                            label = "Bedtime",
                            minute = bed,
                            defaultMinute = 23 * 60,
                            modifier = Modifier.weight(1f)
                        ) { m ->
                            scope.launch {
                                dao.upsert(
                                    current.copy(
                                        bedtimeMinute = m,
                                        sleepMinutes = minutesBetween(m, wake, current.sleepMinutes)
                                    )
                                )
                            }
                        }
                        TimeButton(
                            label = "Wake up",
                            minute = wake,
                            defaultMinute = 6 * 60,
                            modifier = Modifier.weight(1f)
                        ) { m ->
                            scope.launch {
                                dao.upsert(
                                    current.copy(
                                        wakeMinute = m,
                                        sleepMinutes = minutesBetween(bed, m, current.sleepMinutes)
                                    )
                                )
                            }
                        }
                    }
                    Text(
                        text = if (current.sleepMinutes > 0) "Slept " + formatSleepMinutes(current.sleepMinutes)
                        else "Set bedtime and wake time",
                        color = c.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    RatingRow("Sleep quality", current.sleepQuality) { v ->
                        scope.launch { dao.upsert(current.copy(sleepQuality = if (current.sleepQuality == v) 0 else v)) }
                    }
                    if (current.sleepMinutes > 0 || bed >= 0 || wake >= 0) {
                        Text(
                            text = "Clear sleep",
                            color = c.accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickNoRipple {
                                scope.launch {
                                    dao.upsert(
                                        current.copy(bedtimeMinute = -1, wakeMinute = -1, sleepMinutes = 0, sleepQuality = 0)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeButton(
    label: String,
    minute: Int,
    defaultMinute: Int,
    modifier: Modifier = Modifier,
    onPick: (Int) -> Unit
) {
    val c = Growth.colors
    val context = LocalContext.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(c.track)
            .clickNoRipple {
                val start = if (minute >= 0) minute else defaultMinute
                TimePickerDialog(
                    context,
                    { _, hour, min -> onPick(hour * 60 + min) },
                    start / 60,
                    start % 60,
                    false
                ).show()
            }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text = label, color = c.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = if (minute >= 0) formatMinute(minute) else "Set",
            color = if (minute >= 0) c.textPrimary else c.accent,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

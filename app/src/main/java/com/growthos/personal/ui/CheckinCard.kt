package com.growthos.personal.ui

import androidx.compose.foundation.background
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sleep",
                        color = c.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SoftButton(text = "-30m", onClick = {
                            val s = current.sleepMinutes
                            scope.launch {
                                dao.upsert(current.copy(sleepMinutes = if (s <= 30) 0 else s - 30))
                            }
                        })
                        Text(
                            text = formatSleepMinutes(current.sleepMinutes),
                            color = c.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        SoftButton(text = "+30m", onClick = {
                            val s = current.sleepMinutes
                            scope.launch {
                                dao.upsert(
                                    current.copy(sleepMinutes = if (s == 0) 420 else minOf(s + 30, 960))
                                )
                            }
                        })
                    }
                }
            }
        }
    }
}

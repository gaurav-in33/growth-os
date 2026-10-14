package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import java.time.LocalDate

/** Track > Health. */
@Composable
fun HealthSection() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).checkinDao() }
    val today = remember { LocalDate.now().toEpochDay() }
    val flow = remember(dao) { dao.observeBetween(today - 6, today) }
    val week by flow.collectAsState(initial = emptyList())
    val letters = remember { weekLetters() }

    val todays = week.firstOrNull { it.epochDay == today }
    val sleepToday = todays?.sleepMinutes ?: 0
    val perDay = (6 downTo 0).map { back ->
        val day = today - back
        week.firstOrNull { it.epochDay == day }?.sleepMinutes ?: 0
    }
    val slept = perDay.filter { it > 0 }
    val avgSleep = if (slept.isEmpty()) 0 else slept.sum() / slept.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RingProgress(
                        progress = sleepToday / 480f,
                        diameter = 58.dp,
                        stroke = 6.dp
                    ) {}
                    Column {
                        Text(
                            text = "Sleep",
                            color = c.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (sleepToday == 0) "-" else formatSleepMinutes(sleepToday),
                            color = c.textPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
            GlassCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Avg sleep, 7 days",
                        color = c.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (avgSleep == 0) "-" else formatSleepMinutes(avgSleep),
                        color = c.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = "Mood",
                value = if ((todays?.mood ?: 0) == 0) "-" else "${todays?.mood}",
                unit = "/ 5",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Energy",
                value = if ((todays?.energy ?: 0) == 0) "-" else "${todays?.energy}",
                unit = "/ 5",
                modifier = Modifier.weight(1f)
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TileLabel("Sleep, last 7 days")
                BarChart(values = perDay, labels = letters)
            }
        }

        CheckinCard(compact = false)
    }
}

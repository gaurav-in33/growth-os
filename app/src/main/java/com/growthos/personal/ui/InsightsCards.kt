package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ScoreCard() {
    val c = Growth.colors
    val s = rememberDayStats()

    GlassCard(modifier = Modifier.fillMaxWidth(), glow = true) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RingProgress(progress = s.score / 100f, diameter = 104.dp, stroke = 11.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = s.score.toString(),
                            color = c.textPrimary,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(text = "of 100", color = c.textSecondary, fontSize = 11.sp)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Level ${s.level}",
                        color = c.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    ThinBar(progress = s.levelFraction)
                    Text(
                        text = "${s.xpToNext} XP to level ${s.level + 1}",
                        color = c.textSecondary,
                        fontSize = 12.5.sp
                    )
                }
            }
            s.parts.forEach { p ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row {
                        Text(
                            text = p.label,
                            color = c.textPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = p.earned.toString() + " / " + p.max,
                            color = c.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    ThinBar(progress = if (p.max == 0) 0f else p.earned.toFloat() / p.max)
                }
            }
            Text(
                text = "Score and XP come only from what you recorded.",
                color = c.textTertiary,
                fontSize = 12.sp
            )
        }
    }
}


package com.growthos.personal.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.WaterDay
import java.time.LocalDate
import kotlinx.coroutines.launch

private const val WATER_TARGET = 8
private const val WATER_MAX = 20

@Composable
fun WaterCard(modifier: Modifier = Modifier) {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).waterDao() }
    val today = remember { LocalDate.now().toEpochDay() }
    val glassesFlow = remember(dao) { dao.observe(today) }
    val stored by glassesFlow.collectAsState(initial = null)
    val count = stored ?: 0
    val scope = rememberCoroutineScope()
    val fill by animateFloatAsState(
        targetValue = (count.toFloat() / WATER_TARGET).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 400),
        label = "waterFill"
    )

    GlassCard(modifier = modifier, padding = 0.dp) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(fill)
                    .background(Brush.verticalGradient(listOf(c.water1, c.water2)))
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TileLabel("Water")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = count.toString(),
                        color = c.textPrimary,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "/$WATER_TARGET",
                        color = c.textSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                    )
                }
                Box(modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.solid)
                            .clickNoRipple {
                                if (count > 0) {
                                    scope.launch { dao.upsert(WaterDay(today, count - 1)) }
                                }
                            }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "−",
                            color = c.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.solid)
                            .clickNoRipple {
                                if (count < WATER_MAX) {
                                    scope.launch { dao.upsert(WaterDay(today, count + 1)) }
                                }
                            }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Glass",
                            color = c.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

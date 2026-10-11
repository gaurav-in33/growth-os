package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.WaterDay
import java.time.LocalDate
import kotlinx.coroutines.launch

private const val WATER_TARGET = 8
private const val WATER_MAX = 20

@Composable
fun WaterCard() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).waterDao() }
    val today = remember { LocalDate.now().toEpochDay() }
    val glassesFlow = remember(dao) { dao.observe(today) }
    val stored by glassesFlow.collectAsState(initial = null)
    val count = stored ?: 0
    val scope = rememberCoroutineScope()

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "Water", color = c.textPrimary, fontSize = 18.sp)
            Text(
                text = "$count of $WATER_TARGET glasses",
                color = c.textSecondary,
                fontSize = 14.sp
            )
            LinearProgressIndicator(
                progress = { (count.toFloat() / WATER_TARGET).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = c.accent,
                trackColor = c.glassBorder
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (count < WATER_MAX) {
                            scope.launch { dao.upsert(WaterDay(today, count + 1)) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.accent,
                        contentColor = Color.White
                    )
                ) {
                    Text("+ Add glass")
                }
                TextButton(
                    onClick = {
                        if (count > 0) {
                            scope.launch { dao.upsert(WaterDay(today, count - 1)) }
                        }
                    },
                    enabled = count > 0
                ) {
                    Text("Undo one", color = c.textSecondary)
                }
            }
        }
    }
}

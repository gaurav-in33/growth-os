package com.growthos.personal.reminders

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.ui.GlassBackground
import com.growthos.personal.ui.GlowButton
import com.growthos.personal.ui.GrowthTheme
import com.growthos.personal.ui.Growth
import com.growthos.personal.ui.SoftButton
import java.time.LocalTime
import kotlinx.coroutines.delay

/** Full-screen alarm screen that opens over the lock screen. */
class AlarmActivity : ComponentActivity() {

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        val test = intent.getBooleanExtra("test", false)
        setContent {
            GrowthTheme {
                GlassBackground {
                    AlarmScreen(
                        test = test,
                        onAwake = {
                            sendBroadcast(
                                Intent(this@AlarmActivity, AlarmReceiver::class.java)
                                    .setAction(ReminderScheduler.ACTION_AWAKE)
                                    .putExtra("test", test)
                            )
                            finish()
                        },
                        onSnooze = {
                            sendBroadcast(
                                Intent(this@AlarmActivity, AlarmReceiver::class.java)
                                    .setAction(ReminderScheduler.ACTION_SNOOZE)
                                    .putExtra("type", "wake")
                                    .putExtra("test", test)
                            )
                            finish()
                        },
                        onGone = { finish() }
                    )
                }
            }
        }
    }
}

private fun clockText(): String = formatMinute(LocalTime.now().let { it.hour * 60 + it.minute })

@Composable
private fun AlarmScreen(
    test: Boolean,
    onAwake: () -> Unit,
    onSnooze: () -> Unit,
    onGone: () -> Unit
) {
    val c = Growth.colors
    var timeText by remember { mutableStateOf(clockText()) }

    LaunchedEffect(Unit) {
        while (true) {
            timeText = clockText()
            delay(1000)
        }
    }
    // Close this screen when the alarm was stopped from the notification instead.
    LaunchedEffect(Unit) {
        delay(2500)
        while (true) {
            if (!AlarmService.ringing) {
                onGone()
                break
            }
            delay(700)
        }
    }

    val transition = rememberInfiniteTransition(label = "alarm")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "breathe"
    )
    val parts = timeText.split(" ")
    val clock = parts.getOrElse(0) { timeText }
    val ampm = parts.getOrElse(1) { "" }
    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = if (test) "TEST ALARM" else "WAKE UP",
            color = c.accent,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 4.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(modifier = Modifier.size(320.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxR = this.size.minDimension / 2f
                for (k in 0 until 3) {
                    val p = (pulse + k / 3f) % 1f
                    drawCircle(
                        color = c.accent.copy(alpha = (1f - p) * 0.4f),
                        radius = maxR * (0.5f + 0.5f * p),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
                val glowR = maxR * 0.58f * breathe
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(c.accent.copy(alpha = 0.30f), Color.Transparent),
                        center = center,
                        radius = glowR
                    ),
                    radius = glowR
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = clock,
                    color = c.textPrimary,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = ampm,
                    color = c.accent,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp
                )
            }
        }
        Text(
            text = dateText,
            color = c.textSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.weight(1f))

        val bigShape = RoundedCornerShape(44.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .shadow(18.dp, bigShape, ambientColor = c.glow, spotColor = c.glow)
                .clip(bigShape)
                .background(Brush.linearGradient(listOf(c.accent2, c.accent)))
                .clickable(role = Role.Button, onClick = onAwake),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "I'm awake",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        val softShape = RoundedCornerShape(32.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(softShape)
                .background(c.glassFill)
                .border(BorderStroke(1.5.dp, c.accent.copy(alpha = 0.5f)), softShape)
                .clickable(role = Role.Button, onClick = onSnooze),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Snooze 5 min",
                color = c.accent,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

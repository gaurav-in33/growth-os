package com.growthos.personal.reminders

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (test) "Test alarm" else "Wake up!",
            color = c.accent,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = timeText,
            color = c.textPrimary,
            fontSize = 56.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(56.dp))
        GlowButton(
            text = "I'm awake",
            onClick = onAwake,
            modifier = Modifier.fillMaxWidth().height(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        SoftButton(
            text = "Snooze 5 min",
            onClick = onSnooze,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )
    }
}

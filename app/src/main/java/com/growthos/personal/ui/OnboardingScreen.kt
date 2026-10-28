package com.growthos.personal.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.R
import com.growthos.personal.data.RoutineTemplates

/** Goal chosen in onboarding: title + horizon (Daily, Weekly or Long-term). */
class OnboardingGoal(val title: String, val horizon: String)

class OnboardingResult(
    val name: String,
    val goals: List<OnboardingGoal>,
    /** true = seed the routine now, false = user turned it off, null = untouched (default seeding stays) */
    val useRoutine: Boolean?
)

private val GOAL_IDEAS: List<OnboardingGoal> = listOf(
    OnboardingGoal("Wake up at 5 AM", "Daily"),
    OnboardingGoal("Code every day", "Daily"),
    OnboardingGoal("Drink enough water", "Daily"),
    OnboardingGoal("Exercise 4 times a week", "Weekly"),
    OnboardingGoal("Finish my computer course", "Long-term"),
    OnboardingGoal("Build and launch an app", "Long-term"),
    OnboardingGoal("Save money every month", "Long-term"),
    OnboardingGoal("Read 10 pages daily", "Daily")
)

@Composable
fun OnboardingScreen(onDone: (OnboardingResult) -> Unit) {
    val c = Growth.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var picked by remember { mutableStateOf<List<OnboardingGoal>>(emptyList()) }
    var customText by remember { mutableStateOf("") }
    var useRoutine by remember { mutableStateOf(true) }
    var routineTouched by remember { mutableStateOf(false) }
    var notifGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notifGranted = granted }

    fun finish() {
        onDone(
            OnboardingResult(
                name = name.trim(),
                goals = picked,
                useRoutine = if (routineTouched) useRoutine else null
            )
        )
    }

    fun next() {
        if (step >= 3) finish() else step += 1
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "Growth OS logo",
            modifier = Modifier
                .size(if (step == 0) 110.dp else 64.dp)
                .clip(RoundedCornerShape(24.dp))
        )
        Text(
            text = "Growth OS",
            color = c.textPrimary,
            fontSize = 28.sp,
            modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            text = "Step " + (step + 1) + " of 4",
            color = c.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (step == 0) {
                    Text(text = "What should I call you?", color = c.textPrimary, fontSize = 17.sp)
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorText = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Your name (optional)") },
                        isError = errorText != null,
                        supportingText = {
                            val message = errorText
                            if (message != null) Text(message)
                        },
                        colors = glassFieldColors()
                    )
                } else if (step == 1) {
                    Text(text = "Pick your goals", color = c.textPrimary, fontSize = 17.sp)
                    GOAL_IDEAS.chunked(2).forEach { rowGoals ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowGoals.forEach { g ->
                                val on = picked.any { it.title == g.title }
                                MiniChip(
                                    label = g.title,
                                    selected = on,
                                    onClick = {
                                        picked = if (on) picked.filter { it.title != g.title } else picked + g
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowGoals.size == 1) {
                                Column(modifier = Modifier.weight(1f)) { }
                            }
                        }
                    }
                    picked.filter { p -> GOAL_IDEAS.none { it.title == p.title } }.forEach { g ->
                        MiniChip(
                            label = g.title + "  ✕",
                            selected = true,
                            onClick = { picked = picked.filter { it.title != g.title } }
                        )
                    }
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("My own goal") },
                        colors = glassFieldColors()
                    )
                    SoftButton(
                        text = "Add my goal",
                        onClick = {
                            val t = customText.trim()
                            if (t.isNotEmpty() && t.length <= 60 && picked.none { it.title == t }) {
                                picked = picked + OnboardingGoal(t, "Long-term")
                                customText = ""
                            }
                        }
                    )
                } else if (step == 2) {
                    Text(text = "Your daily routine", color = c.textPrimary, fontSize = 17.sp)
                    RoutineTemplates.MY_DAY.forEach { item ->
                        val h = item.first / 60
                        val m = item.first % 60
                        val hh = if (h % 12 == 0) 12 else h % 12
                        val mm = if (m < 10) "0" + m else m.toString()
                        val ap = if (h < 12) "AM" else "PM"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "" + hh + ":" + mm + " " + ap,
                                color = c.textSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(text = item.second, color = c.textPrimary, fontSize = 14.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Use this routine",
                            color = c.textPrimary,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = useRoutine,
                            onCheckedChange = {
                                useRoutine = it
                                routineTouched = true
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = c.accent,
                                checkedThumbColor = Color.White
                            )
                        )
                    }
                    Text(
                        text = "You can edit it anytime in Plan.",
                        color = c.textTertiary,
                        fontSize = 12.sp
                    )
                } else {
                    Text(text = "Reminders", color = c.textPrimary, fontSize = 17.sp)
                    Text(
                        text = "Routine, task and alarm reminders need notifications.",
                        color = c.textSecondary,
                        fontSize = 14.sp
                    )
                    val needs = Build.VERSION.SDK_INT >= 33 && !notifGranted
                    if (needs) {
                        GlowButton(
                            text = "Allow notifications",
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        TagPill(text = "Notifications ready")
                    }
                }

                GlowButton(
                    text = if (step >= 3) "Finish" else "Next",
                    onClick = {
                        if (step == 0 && name.trim().length > 30) {
                            errorText = "Keep it under 30 characters"
                        } else {
                            next()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = {
                        if (step == 0) name = ""
                        if (step == 1) picked = emptyList()
                        if (step == 2) routineTouched = false
                        next()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip", color = c.textSecondary)
                }
            }
        }
    }
}

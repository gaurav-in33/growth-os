package com.growthos.personal

import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.draw.clipToBounds
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.GoalEntity
import com.growthos.personal.data.ProfileStore
import com.growthos.personal.data.RoutineItemEntity
import com.growthos.personal.data.RoutineTemplates
import java.time.LocalDate
import kotlinx.coroutines.launch
import com.growthos.personal.data.SettingsStore
import com.growthos.personal.ui.ThemePrefs
import com.growthos.personal.ui.resolveDark
import com.growthos.personal.ui.GlassBackground
import com.growthos.personal.ui.GlassCard
import com.growthos.personal.ui.GlassNavBar
import com.growthos.personal.ui.Growth
import com.growthos.personal.ui.GrowthTheme
import com.growthos.personal.ui.NavTab
import com.growthos.personal.ui.OnboardingScreen
import com.growthos.personal.ui.ProfileScreen
import com.growthos.personal.reminders.ReminderScheduler
import com.growthos.personal.ui.GrowScreen
import com.growthos.personal.ui.PlanScreen
import com.growthos.personal.ui.TodayScreen
import com.growthos.personal.ui.TrackScreen

class MainActivity : ComponentActivity() {
    override fun onPause() {
        super.onPause()
        TodayWidget.refresh(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePrefs.mode = SettingsStore(this).themeMode()
        setContent {
            val dark = resolveDark()
            DisposableEffect(dark) {
                this@MainActivity.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { dark },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { dark }
                )
                onDispose { }
            }
            GrowthTheme {
                GrowthApp(initialTab = intent.getIntExtra("open_tab", 0).coerceIn(0, 4))
            }
        }
    }
}

@Composable
fun GrowthApp(initialTab: Int = 0) {
    val context = LocalContext.current
    val store = remember { ProfileStore(context) }
    var onboarded by remember { mutableStateOf(store.isOnboardingDone()) }
    var name by remember { mutableStateOf(store.name()) }
    var selected by rememberSaveable { mutableIntStateOf(initialTab) }
    val scope = rememberCoroutineScope()

    // `selected` moves the nav bubble at once; `shown` swaps the screen one frame later.
    var shown by rememberSaveable { mutableIntStateOf(initialTab) }
    // Tabs stay alive once opened (hidden at 0 size), so switching is instant.
    val visited = remember { mutableStateListOf(shown) }
    var dayKey by remember { mutableLongStateOf(LocalDate.now().toEpochDay()) }

    LaunchedEffect(selected) {
        withFrameNanos { }
        shown = selected
        if (selected !in visited) visited.add(selected)
    }

    // Warm the other tabs in the background after start.
    LaunchedEffect(onboarded) {
        if (onboarded) {
            delay(1200)
            for (i in 0..4) {
                if (i !in visited) visited.add(i)
                delay(400)
            }
        }
    }

    // New day: rebuild the tabs so dates and counters refresh.
    val activity = context as? ComponentActivity
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val today = LocalDate.now().toEpochDay()
                if (today != dayKey) dayKey = today
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { ReminderScheduler.rescheduleAll(context) }
    }

    LaunchedEffect(Unit) {
        if (store.name() == "German") {
            store.setName("Gourav")
            name = "Gourav"
        }
    }

    val tabs = listOf(
        NavTab("Today", Icons.Filled.Home),
        NavTab("Plan", Icons.AutoMirrored.Filled.List),
        NavTab("Track", Icons.Filled.CheckCircle),
        NavTab("Grow", Icons.Filled.Star),
        NavTab("More", Icons.Filled.Menu)
    )

    GlassBackground {
        if (!onboarded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                OnboardingScreen(onDone = { result ->
                    store.setName(result.name)
                    name = result.name
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val db = AppDatabase.get(context)
                            val today = LocalDate.now().toEpochDay()
                            for (g in result.goals) {
                                db.goalDao().insert(
                                    GoalEntity(
                                        title = g.title,
                                        horizon = g.horizon,
                                        progress = 0,
                                        createdEpochDay = today
                                    )
                                )
                            }
                            val settings = SettingsStore(context)
                            if (result.useRoutine == true) {
                                if (db.routineDao().count() == 0) {
                                    db.routineDao().insertAll(
                                        RoutineTemplates.MY_DAY.map {
                                            RoutineItemEntity(title = it.second, startMinute = it.first)
                                        }
                                    )
                                }
                                settings.setRoutineSeeded()
                            } else if (result.useRoutine == false) {
                                settings.setRoutineSeeded()
                            }
                        }
                        store.setOnboardingDone()
                        onboarded = true
                    }
                })
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to Color.Black,
                                    0.95f to Color.Black,
                                    1f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                ) {
                    for (i in visited.toList()) {
                        key(i, dayKey) {
                            Box(
                                modifier = if (i == shown) Modifier.fillMaxSize()
                                else Modifier.size(0.dp).clipToBounds()
                            ) {
                                when (i) {
                                    0 -> TodayScreen(name = name, onOpenPlan = { selected = 1 })
                                    1 -> PlanScreen()
                                    2 -> TrackScreen()
                                    3 -> GrowScreen()
                                    4 -> ProfileScreen(
                                        name = name,
                                        onSaveName = { newName ->
                                            store.setName(newName)
                                            name = newName
                                        }
                                    )
                                    else -> PlaceholderScreen(title = tabs[i].label)
                                }
                            }
                        }
                    }
                }
                GlassNavBar(
                    tabs = tabs,
                    selected = selected,
                    onSelect = { selected = it },
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    val c = Growth.colors
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = title, color = c.textPrimary, fontSize = 26.sp)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "This section is coming in a later step.",
                color = c.textSecondary,
                fontSize = 15.sp
            )
        }
    }
}

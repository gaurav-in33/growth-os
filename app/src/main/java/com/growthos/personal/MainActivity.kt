package com.growthos.personal

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.ProfileStore
import com.growthos.personal.ui.GlassBackground
import com.growthos.personal.ui.GlassCard
import com.growthos.personal.ui.GlassNavBar
import com.growthos.personal.ui.Growth
import com.growthos.personal.ui.GrowthTheme
import com.growthos.personal.ui.NavTab
import com.growthos.personal.ui.OnboardingScreen
import com.growthos.personal.ui.ProfileScreen
import com.growthos.personal.ui.TodayScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GrowthTheme {
                GrowthApp()
            }
        }
    }
}

@Composable
fun GrowthApp() {
    val context = LocalContext.current
    val store = remember { ProfileStore(context) }
    var onboarded by remember { mutableStateOf(store.isOnboardingDone()) }
    var name by remember { mutableStateOf(store.name()) }
    var selected by rememberSaveable { mutableIntStateOf(0) }

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
                OnboardingScreen(onDone = { enteredName ->
                    store.setName(enteredName)
                    store.setOnboardingDone()
                    name = enteredName
                    onboarded = true
                })
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding()
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (selected) {
                        0 -> TodayScreen(name = name)
                        4 -> ProfileScreen(
                            name = name,
                            onSaveName = { newName ->
                                store.setName(newName)
                                name = newName
                            }
                        )
                        else -> PlaceholderScreen(title = tabs[selected].label)
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

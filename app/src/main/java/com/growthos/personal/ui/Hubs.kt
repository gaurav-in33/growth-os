package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TrackScreen() {
    var section by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenTitle("Track")
            Segmented(listOf("Habits", "Health", "Fitness"), section, { section = it })
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (section) {
                0 -> HabitsSection()
                1 -> HealthSection()
                else -> FitnessSection()
            }
        }
    }
}

@Composable
fun GrowScreen() {
    var section by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenTitle("Grow")
            Segmented(listOf("Goals", "Learn", "Projects"), section, { section = it })
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (section) {
                0 -> GoalsSection()
                1 -> LearnSection()
                else -> ProjectsSection()
            }
        }
    }
}

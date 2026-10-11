package com.growthos.personal.ui

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun LogChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = Growth.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            labelColor = c.textPrimary,
            selectedContainerColor = c.accent,
            selectedLabelColor = Color.White
        )
    )
}

fun formatSleepMinutes(minutes: Int): String =
    if (minutes <= 0) "Not logged" else "${minutes / 60}h ${minutes % 60}m"

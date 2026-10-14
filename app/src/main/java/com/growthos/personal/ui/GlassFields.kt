package com.growthos.personal.ui

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable

@Composable
fun glassFieldColors(): TextFieldColors {
    val c = Growth.colors
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = c.textPrimary,
        unfocusedTextColor = c.textPrimary,
        focusedBorderColor = c.accent,
        unfocusedBorderColor = c.textTertiary,
        cursorColor = c.accent,
        focusedPlaceholderColor = c.textSecondary,
        unfocusedPlaceholderColor = c.textSecondary,
        focusedLabelColor = c.accent,
        unfocusedLabelColor = c.textSecondary
    )
}

package com.growthos.personal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class GrowthColors(
    val isDark: Boolean,
    val bgTop: Color,
    val bgBottom: Color,
    val blobA: Color,
    val blobB: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val glassFill: Color,
    val glassBorder: Color
)

val DarkColors = GrowthColors(
    isDark = true,
    bgTop = Color(0xFF070B16),
    bgBottom = Color(0xFF0E1A33),
    blobA = Color(0xFF2D5BFF),
    blobB = Color(0xFF7A3CFF),
    accent = Color(0xFF6C9BFF),
    textPrimary = Color(0xFFF2F5FF),
    textSecondary = Color(0xFFA7B1CC),
    glassFill = Color.White.copy(alpha = 0.08f),
    glassBorder = Color.White.copy(alpha = 0.22f)
)

val LightColors = GrowthColors(
    isDark = false,
    bgTop = Color(0xFFF7F4FF),
    bgBottom = Color(0xFFEAF0FF),
    blobA = Color(0xFFFFB27A),
    blobB = Color(0xFFB9A7FF),
    accent = Color(0xFFFF8A3D),
    textPrimary = Color(0xFF1B1B2F),
    textSecondary = Color(0xFF5B6075),
    glassFill = Color.White.copy(alpha = 0.55f),
    glassBorder = Color.White.copy(alpha = 0.9f)
)

val LocalGrowthColors = staticCompositionLocalOf { LightColors }

object Growth {
    val colors: GrowthColors
        @Composable
        get() = LocalGrowthColors.current
}

object ThemePrefs {
    // 0 = follow system, 1 = light, 2 = dark
    var mode by mutableIntStateOf(0)
}

@Composable
fun resolveDark(): Boolean = when (ThemePrefs.mode) {
    1 -> false
    2 -> true
    else -> isSystemInDarkTheme()
}

@Composable
fun GrowthTheme(content: @Composable () -> Unit) {
    val colors = if (resolveDark()) DarkColors else LightColors
    CompositionLocalProvider(LocalGrowthColors provides colors) {
        content()
    }
}

@Composable
fun GlassBackground(content: @Composable BoxScope.() -> Unit) {
    val c = Growth.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerA = Offset(size.width * 0.15f, size.height * 0.18f)
            val centerB = Offset(size.width * 0.9f, size.height * 0.72f)
            val radius = size.width * 0.75f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(c.blobA.copy(alpha = 0.45f), Color.Transparent),
                    center = centerA,
                    radius = radius
                ),
                radius = radius,
                center = centerA
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(c.blobB.copy(alpha = 0.40f), Color.Transparent),
                    center = centerB,
                    radius = radius
                ),
                radius = radius,
                center = centerB
            )
        }
        content()
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = Growth.colors
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(c.glassFill)
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(listOf(c.glassBorder, Color.Transparent))
                ),
                shape
            )
            .padding(16.dp)
    ) {
        content()
    }
}

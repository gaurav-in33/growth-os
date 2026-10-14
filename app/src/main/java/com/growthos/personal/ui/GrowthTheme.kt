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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class GrowthColors(
    val isDark: Boolean,
    val bgTop: Color,
    val bgBottom: Color,
    val blobA: Color,
    val blobB: Color,
    val blobC: Color,
    val accent: Color,
    val accent2: Color,
    val rose: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val glassFill: Color,
    val glassBorder: Color,
    val edge: Color,
    val track: Color,
    val tag: Color,
    val glow: Color,
    val tint: Color,
    val solid: Color,
    val navFill: Color,
    val sheenA: Color,
    val sheenB: Color,
    val sheenC: Color,
    val water1: Color,
    val water2: Color
)

val LightColors = GrowthColors(
    isDark = false,
    bgTop = Color(0xFFFDF9FF),
    bgBottom = Color(0xFFEDF1FF),
    blobA = Color(0xFFFFA06E).copy(alpha = 0.70f),
    blobB = Color(0xFFB496FF).copy(alpha = 0.55f),
    blobC = Color(0xFFFF82BE).copy(alpha = 0.30f),
    accent = Color(0xFFFF7A2F),
    accent2 = Color(0xFFFFA45C),
    rose = Color(0xFFFF5C8A),
    textPrimary = Color(0xFF1D1B2E),
    textSecondary = Color(0xFF5C6078),
    textTertiary = Color(0xFF9A9EB0),
    glassFill = Color.White.copy(alpha = 0.34f),
    glassBorder = Color(0xFF3C3264).copy(alpha = 0.16f),
    edge = Color.White.copy(alpha = 0.85f),
    track = Color(0xFF3C3264).copy(alpha = 0.09f),
    tag = Color(0xFFFF7A2F).copy(alpha = 0.13f),
    glow = Color(0xFFFF7A2F).copy(alpha = 0.50f),
    tint = Color(0xFFFF7A2F).copy(alpha = 0.16f),
    solid = Color.White,
    navFill = Color.White.copy(alpha = 0.92f),
    sheenA = Color.White.copy(alpha = 0.66f),
    sheenB = Color.White.copy(alpha = 0.24f),
    sheenC = Color.White.copy(alpha = 0.46f),
    water1 = Color(0xFF569CFF).copy(alpha = 0.30f),
    water2 = Color(0xFF7878FF).copy(alpha = 0.34f)
)

val DarkColors = GrowthColors(
    isDark = true,
    bgTop = Color(0xFF0F0C24),
    bgBottom = Color(0xFF171C3C),
    blobA = Color(0xFFFF6A28).copy(alpha = 0.40f),
    blobB = Color(0xFF6246FF).copy(alpha = 0.44f),
    blobC = Color(0xFFFF5096).copy(alpha = 0.22f),
    accent = Color(0xFFFF8A45),
    accent2 = Color(0xFFFFB070),
    rose = Color(0xFFFF6F9C),
    textPrimary = Color(0xFFF6F4FF),
    textSecondary = Color(0xFFBAB6D6),
    textTertiary = Color(0xFF7B77A0),
    glassFill = Color(0xFF201E42).copy(alpha = 0.42f),
    glassBorder = Color.White.copy(alpha = 0.14f),
    edge = Color.White.copy(alpha = 0.12f),
    track = Color.White.copy(alpha = 0.11f),
    tag = Color(0xFFFF8A45).copy(alpha = 0.17f),
    glow = Color(0xFFFF8A45).copy(alpha = 0.58f),
    tint = Color(0xFFFF8A45).copy(alpha = 0.18f),
    solid = Color(0xFF2A2650),
    navFill = Color(0xFF1A1834).copy(alpha = 0.94f),
    sheenA = Color.White.copy(alpha = 0.15f),
    sheenB = Color.White.copy(alpha = 0.03f),
    sheenC = Color.White.copy(alpha = 0.07f),
    water1 = Color(0xFF569CFF).copy(alpha = 0.34f),
    water2 = Color(0xFF8C78FF).copy(alpha = 0.30f)
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
            val radius = size.width * 1.0f
            val centerA = Offset(0f, size.height * 0.04f)
            val centerB = Offset(size.width, size.height * 0.58f)
            val centerC = Offset(0f, size.height)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(c.blobA, Color.Transparent),
                    center = centerA,
                    radius = radius
                ),
                radius = radius,
                center = centerA
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(c.blobB, Color.Transparent),
                    center = centerB,
                    radius = radius
                ),
                radius = radius,
                center = centerB
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(c.blobC, Color.Transparent),
                    center = centerC,
                    radius = radius
                ),
                radius = radius,
                center = centerC
            )
        }
        content()
    }
}

/**
 * Frosted glass surface: see-through fill, a soft light sheen, a thin bright edge
 * and a soft coloured shadow (stronger when glow = true).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glow: Boolean = false,
    padding: Dp = 16.dp,
    cornerRadius: Dp = 26.dp,
    highlight: Boolean = false,
    content: @Composable () -> Unit
) {
    val c = Growth.colors
    val shape = RoundedCornerShape(cornerRadius)
    val shadowColor = if (glow) c.glow else c.tint
    Box(
        modifier = modifier
            .shadow(
                elevation = if (glow) 16.dp else 8.dp,
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .background(c.glassFill, shape)
            .background(Brush.linearGradient(listOf(c.sheenA, c.sheenB, c.sheenC)), shape)
            .border(
                if (highlight) BorderStroke(1.5.dp, c.accent)
                else BorderStroke(
                    1.dp,
                    Brush.linearGradient(listOf(c.edge, c.edge.copy(alpha = 0.15f)))
                ),
                shape
            )
            .padding(padding)
    ) {
        content()
    }
}

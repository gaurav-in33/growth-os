package com.growthos.personal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ChartPurple = Color(0xFF6C63FF)
val ChartGreen = Color(0xFF2EC4B6)
val ChartRose = Color(0xFFFF5C8A)
val ChartBlue = Color(0xFF3A86FF)

/** Bar chart. Pass labels as "" for bars that should have no label. */
@Composable
fun ValueBars(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 96.dp,
    showValues: Boolean = true,
    color: Color? = null,
    valueText: (Float) -> String = { it.toInt().toString() }
) {
    val c = Growth.colors
    val barColor = color ?: c.accent
    val maxValue = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    val gap = if (values.size > 12) 2.dp else 8.dp
    val usable = (height - 16.dp).value
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        values.forEachIndexed { index, v ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().height(height),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (showValues && v > 0f) {
                        Text(
                            text = valueText(v),
                            color = c.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    if (v <= 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(c.track)
                        )
                    } else {
                        val fraction = (v / maxValue).coerceIn(0.06f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((usable * fraction).dp)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                                .background(Brush.verticalGradient(listOf(barColor.copy(alpha = 0.75f), barColor)))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = labels.getOrElse(index) { "" },
                    color = c.textTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/** Line chart for values between 0 and maxValue. A value of 0 means "not logged" and leaves a gap. */
@Composable
fun TrendLines(
    series: List<List<Float>>,
    colors: List<Color>,
    labels: List<String>,
    maxValue: Float,
    modifier: Modifier = Modifier,
    height: Dp = 90.dp
) {
    val c = Growth.colors
    val gridColor = c.track
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            val w = this.size.width
            val h = this.size.height
            val n = labels.size.coerceAtLeast(1)
            for (g in 0..4) {
                val y = h * g / 4f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            val pad = 6.dp.toPx()
            series.forEachIndexed { si, values ->
                val col = colors.getOrElse(si) { Color.Gray }
                var prev: Offset? = null
                values.forEachIndexed { i, v ->
                    if (v > 0f) {
                        val x = w * (i + 0.5f) / n
                        val y = h - pad - (v / maxValue).coerceIn(0f, 1f) * (h - pad * 2f)
                        val point = Offset(x, y)
                        val before = prev
                        if (before != null) {
                            drawLine(
                                color = col,
                                start = before,
                                end = point,
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                        drawCircle(color = col, radius = 4.5.dp.toPx(), center = point)
                        prev = point
                    } else {
                        prev = null
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEach { label ->
                Text(
                    text = label,
                    color = c.textTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun DonutChart(
    slices: List<Pair<Float, Color>>,
    diameter: Dp,
    stroke: Dp,
    modifier: Modifier = Modifier,
    center: @Composable BoxScope.() -> Unit = {}
) {
    val c = Growth.colors
    val trackColor = c.track
    val total = slices.sumOf { it.first.toDouble() }.toFloat()
    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val inset = sw / 2f + 2.dp.toPx()
            val topLeft = Offset(inset, inset)
            val arcSize = Size(this.size.width - inset * 2f, this.size.height - inset * 2f)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw)
            )
            if (total > 0f) {
                var start = -90f
                slices.forEach { slice ->
                    val sweep = 360f * slice.first / total
                    drawArc(
                        color = slice.second,
                        startAngle = start,
                        sweepAngle = (sweep - 2f).coerceAtLeast(0.5f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = sw)
                    )
                    start += sweep
                }
            }
        }
        center()
    }
}

@Composable
fun EmojiTile(
    emoji: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    val c = Growth.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.track)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 15.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = c.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Text(
            text = value,
            color = c.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        if (progress != null) ThinBar(progress = progress)
    }
}

@Composable
fun LegendDotLabel(color: Color, label: String) {
    val c = Growth.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(
            text = label,
            color = c.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

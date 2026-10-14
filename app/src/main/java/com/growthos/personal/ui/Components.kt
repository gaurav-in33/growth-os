package com.growthos.personal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun Modifier.clickNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )

fun weekLetters(): List<String> =
    (6 downTo 0).map { back ->
        LocalDate.now().minusDays(back.toLong()).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH)
    }

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    val c = Growth.colors
    Text(
        text = text,
        color = c.textPrimary,
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier
    )
}

@Composable
fun SectionHeader(title: String, trailing: String? = null, onTrailing: (() -> Unit)? = null) {
    val c = Growth.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = c.textPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            val m = if (onTrailing != null) Modifier.clickNoRipple(onTrailing) else Modifier
            Text(
                text = trailing,
                color = if (onTrailing != null) c.accent else c.textSecondary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = m
            )
        }
    }
}

@Composable
fun TileLabel(text: String, trailing: String? = null) {
    val c = Growth.colors
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(c.accent2, c.accent)))
        )
        Text(
            text = text,
            color = c.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 7.dp).weight(1f)
        )
        if (trailing != null) {
            Text(
                text = trailing,
                color = c.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Growth.colors
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, ambientColor = c.tint, spotColor = c.tint)
            .background(c.glassFill, shape)
            .background(Brush.linearGradient(listOf(c.sheenA, c.sheenB, c.sheenC)), shape)
            .padding(5.dp)
    ) {
        options.forEachIndexed { index, label ->
            val on = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(
                        if (on) Brush.linearGradient(listOf(c.accent2, c.accent))
                        else SolidColor(Color.Transparent)
                    )
                    .clickNoRipple { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (on) Color.White else c.textSecondary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun GlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val c = Growth.colors
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .shadow(10.dp, shape, ambientColor = c.glow, spotColor = c.glow)
            .clip(shape)
            .background(
                if (enabled) Brush.linearGradient(listOf(c.accent2, c.accent))
                else SolidColor(c.track)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else c.textTertiary,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Growth.colors
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .background(c.tag)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = c.accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun AddButton(onClick: () -> Unit, modifier: Modifier = Modifier, diameter: Dp = 44.dp) {
    val c = Growth.colors
    Box(
        modifier = modifier
            .size(diameter)
            .shadow(10.dp, CircleShape, ambientColor = c.glow, spotColor = c.glow)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(c.accent2, c.accent)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Add",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun MiniChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Growth.colors
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) c.tag else c.track)
            .clickNoRipple(onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) c.accent else c.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun TagPill(text: String) {
    val c = Growth.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(c.tag)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = c.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun CheckDot(checked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Growth.colors
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(
                if (checked) Brush.linearGradient(listOf(c.accent2, c.accent))
                else SolidColor(Color.Transparent)
            )
            .clickNoRipple(onClick),
        contentAlignment = Alignment.Center
    ) {
        if (!checked) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = c.textTertiary,
                    radius = (this.size.minDimension / 2f) - 1.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        } else {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Done",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
fun ThinBar(progress: Float, modifier: Modifier = Modifier) {
    val c = Growth.colors
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(9.dp)
            .clip(shape)
            .background(c.track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(shape)
                .background(Brush.horizontalGradient(listOf(c.accent, c.accent2)))
        )
    }
}

@Composable
fun RingProgress(
    progress: Float,
    diameter: Dp,
    stroke: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val c = Growth.colors
    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val inset = sw / 2f + 2.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2f, this.size.height - inset * 2f)
            val topLeft = Offset(inset, inset)
            val p = progress.coerceIn(0f, 1f)
            drawArc(
                color = c.track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw)
            )
            if (p > 0f) {
                drawArc(
                    color = c.glow.copy(alpha = 0.25f),
                    startAngle = -90f,
                    sweepAngle = 360f * p,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = sw * 1.9f, cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.linearGradient(listOf(c.accent2, c.rose)),
                    startAngle = -90f,
                    sweepAngle = 360f * p,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = sw, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}

@Composable
fun BarChart(values: List<Int>, labels: List<String>, modifier: Modifier = Modifier) {
    val c = Growth.colors
    val maxValue = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(
        modifier = modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        values.forEachIndexed { index, v ->
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val fraction = if (v <= 0) 0.05f else (v.toFloat() / maxValue).coerceIn(0.08f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp
                                )
                            )
                            .background(
                                if (v > 0) Brush.verticalGradient(listOf(c.accent2, c.accent))
                                else SolidColor(c.track)
                            )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = labels.getOrElse(index) { "" },
                    color = c.textTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun MoreMenu(items: List<Pair<String, () -> Unit>>) {
    val c = Growth.colors
    var open by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable { open = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More",
                tint = c.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEach { (label, action) ->
                DropdownMenuItem(
                    text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        open = false
                        action()
                    }
                )
            }
        }
    }
}

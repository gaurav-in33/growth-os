package com.growthos.personal.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class NavTab(val label: String, val icon: ImageVector)

@Composable
fun GlassNavBar(
    tabs: List<NavTab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Growth.colors
    val shape = RoundedCornerShape(36.dp)
    val barFill = if (c.isDark) Color(0xFF0B0F1A).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.75f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .height(72.dp)
            .clip(shape)
            .background(barFill)
            .border(BorderStroke(1.dp, c.glassBorder), shape)
    ) {
        val itemWidth = maxWidth / tabs.size
        val bubbleX by animateDpAsState(
            targetValue = itemWidth * selected,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = 300f),
            label = "bubbleX"
        )

        Box(
            modifier = Modifier
                .offset(x = bubbleX + (itemWidth - 56.dp) / 2, y = 8.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(c.accent, c.accent.copy(alpha = 0.55f))
                    )
                )
        )

        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (isSelected) Color.White else c.textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    if (!isSelected) {
                        Text(
                            text = tab.label,
                            color = c.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

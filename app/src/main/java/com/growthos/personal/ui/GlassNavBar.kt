package com.growthos.personal.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(72.dp)
            .shadow(14.dp, shape, ambientColor = c.tint, spotColor = c.tint)
            .clip(shape)
            .background(c.navFill)
            .border(
                BorderStroke(1.dp, Brush.linearGradient(listOf(c.edge, c.edge.copy(alpha = 0.2f)))),
                shape
            )
    ) {
        val itemWidth = maxWidth / tabs.size
        // Plain, quick slide (no bounce) so the bubble never lags behind the tap.
        val bubbleX by animateDpAsState(
            targetValue = itemWidth * selected,
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            label = "bubbleX"
        )

        Box(
            modifier = Modifier
                .offset(x = bubbleX + (itemWidth - 54.dp) / 2, y = 9.dp)
                .size(54.dp)
                .shadow(12.dp, CircleShape, ambientColor = c.glow, spotColor = c.glow)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(c.accent2, c.accent)))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent),
                        start = Offset(0f, 0f),
                        end = Offset(110f, 110f)
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
                        .clickNoRipple { onSelect(index) },
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
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

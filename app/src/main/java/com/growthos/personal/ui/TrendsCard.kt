package com.growthos.personal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun fmtMin(m: Int): String =
    if (m >= 60) (m / 60).toString() + "h " + (m % 60).toString() + "m" else m.toString() + "m"

@Composable
private fun ChartBlock(title: String, headline: String, content: @Composable () -> Unit) {
    val c = Growth.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = c.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = headline,
                color = c.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        content()
    }
}

@Composable
private fun Spark(values: List<Float>, color: Color) {
    Canvas(modifier = Modifier.fillMaxWidth().height(26.dp)) {
        if (values.size < 2) return@Canvas
        val maxV = values.maxOrNull() ?: 0f
        val minV = values.minOrNull() ?: 0f
        val range = (maxV - minV).coerceAtLeast(1f)
        val w = this.size.width
        val h = this.size.height
        val pad = 3.dp.toPx()
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = w * i / (values.size - 1)
            val y = h - pad - (v - minV) / range * (h - pad * 2f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun ReportTile(
    label: String,
    value: String,
    color: Color,
    values: List<Float>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Growth.colors
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.12f))
            .then(if (selected) Modifier.border(1.5.dp, color, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, color = c.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(text = value, color = color, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Spark(values, color)
    }
}

@Composable
private fun InsightChart(
    values: List<Float>,
    labels: List<String>,
    dates: List<String>,
    color: Color,
    fmt: (Float) -> String
) {
    val c = Growth.colors
    val trackColor = c.track
    var sel by remember(values.size) { mutableIntStateOf(values.size - 1) }
    val last = (values.size - 1).coerceAtLeast(0)
    val idx = sel.coerceIn(0, last)
    val avgV = if (values.isEmpty()) 0f else values.average().toFloat()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fmt(values.getOrElse(idx) { 0f }),
                    color = color,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(text = dates.getOrElse(idx) { "" }, color = c.textTertiary, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = fmt(avgV), color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text(text = "average", color = c.textTertiary, fontSize = 12.sp)
            }
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .pointerInput(values.size) {
                    detectTapGestures { off ->
                        val n = values.size.coerceAtLeast(1)
                        sel = ((off.x / size.width) * n).toInt().coerceIn(0, n - 1)
                    }
                }
        ) {
            val w = this.size.width
            val h = this.size.height
            val n = values.size.coerceAtLeast(1)
            val pad = 12.dp.toPx()
            val maxV = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
            val line = Path()
            val area = Path()
            values.forEachIndexed { i, v ->
                val x = w * (i + 0.5f) / n
                val y = h - pad - (v / maxV) * (h - pad * 2f)
                if (i == 0) {
                    line.moveTo(x, y)
                    area.moveTo(x, h)
                    area.lineTo(x, y)
                } else {
                    line.lineTo(x, y)
                    area.lineTo(x, y)
                }
            }
            if (values.isNotEmpty()) {
                area.lineTo(w * (values.size - 0.5f) / n, h)
                area.close()
                drawPath(
                    path = area,
                    brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0f)))
                )
            }
            val avgY = h - pad - (avgV / maxV) * (h - pad * 2f)
            drawLine(
                color = color.copy(alpha = 0.45f),
                start = Offset(0f, avgY),
                end = Offset(w, avgY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
            )
            drawPath(
                path = line,
                color = color,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            if (values.isNotEmpty()) {
                val sx = w * (idx + 0.5f) / n
                val sy = h - pad - (values[idx] / maxV) * (h - pad * 2f)
                drawLine(
                    color = trackColor,
                    start = Offset(sx, 0f),
                    end = Offset(sx, h),
                    strokeWidth = 1.dp.toPx()
                )
                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(sx, sy))
                drawCircle(
                    color = color,
                    radius = 6.dp.toPx(),
                    center = Offset(sx, sy),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEach { label ->
                Text(
                    text = label,
                    color = c.textTertiary,
                    fontSize = 10.sp,
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
fun WeeklyReportCard() {
    val c = Growth.colors
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val today = remember { LocalDate.now().toEpochDay() }
    var rangeIndex by rememberSaveable { mutableIntStateOf(0) }
    var metric by rememberSaveable { mutableIntStateOf(1) }
    var more by rememberSaveable { mutableIntStateOf(0) }
    val days = if (rangeIndex == 0) 7 else 30
    val from = today - days + 1
    val fromMillis = remember(days) {
        LocalDate.ofEpochDay(today - days + 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    val tasks by remember(db) { db.taskDao().observeAll() }.collectAsState(initial = emptyList())
    val habitList by remember(db) { db.habitDao().observeActive() }.collectAsState(initial = emptyList())
    val habitChecks by remember(db) { db.habitDao().observeCheckins() }.collectAsState(initial = emptyList())
    val study by remember(db, days) { db.studyDao().observeSince(from) }.collectAsState(initial = emptyList())
    val workouts by remember(db, days) { db.workoutDao().observeSince(from) }.collectAsState(initial = emptyList())
    val checkins by remember(db, days) { db.checkinDao().observeBetween(from, today) }.collectAsState(initial = emptyList())
    val finance by remember(db, days) { db.financeDao().observeBetween(from, today) }.collectAsState(initial = emptyList())
    val waterDays by remember(db, days) { db.waterDao().observeSince(from) }.collectAsState(initial = emptyList())
    val focus by remember(db, days) { db.focusDao().observeSessionsSince(fromMillis) }.collectAsState(initial = emptyList())

    val dayList = (0 until days).map { from + it }
    val labels = dayList.mapIndexed { i, d ->
        val date = LocalDate.ofEpochDay(d)
        if (days == 7) date.dayOfWeek.name.substring(0, 1)
        else if (i % 5 == 0 || i == days - 1) date.dayOfMonth.toString() else ""
    }
    val dateFmt = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
    val dates = dayList.map { LocalDate.ofEpochDay(it).format(dateFmt) }
    val showValues = days == 7

    fun perDay(f: (Long) -> Float): List<Float> = dayList.map { f(it) }

    val tasksDone = perDay { d -> tasks.count { it.isDone && it.completedEpochDay == d }.toFloat() }
    val habitsDone = perDay { d -> habitChecks.count { it.epochDay == d }.toFloat() }
    val focusMin = perDay { d ->
        focus.filter {
            Instant.ofEpochMilli(it.endedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay() == d
        }.sumOf { it.minutes }.toFloat()
    }
    val studyMin = perDay { d -> study.filter { it.epochDay == d }.sumOf { it.minutes }.toFloat() }
    val workoutMin = perDay { d -> workouts.filter { it.epochDay == d }.sumOf { it.minutes }.toFloat() }
    val water = perDay { d -> (waterDays.firstOrNull { it.epochDay == d }?.glasses ?: 0).toFloat() }
    val sleepHours = perDay { d -> (checkins.firstOrNull { it.epochDay == d }?.sleepMinutes ?: 0) / 60f }
    val mood = perDay { d -> (checkins.firstOrNull { it.epochDay == d }?.mood ?: 0).toFloat() }
    val energy = perDay { d -> (checkins.firstOrNull { it.epochDay == d }?.energy ?: 0).toFloat() }
    val spent = perDay { d -> finance.filter { it.epochDay == d && it.kind == "Expense" }.sumOf { it.amountPaise } / 100f }
    val received = perDay { d -> finance.filter { it.epochDay == d && it.kind != "Expense" }.sumOf { it.amountPaise } / 100f }

    // Mood chart: carry the last logged value over days with no check-in.
    var lastMood = 0f
    val moodFilled = mood.map { if (it > 0f) { lastMood = it; it } else lastMood }

    fun avg(v: List<Float>): String {
        val logged = v.filter { it > 0f }
        return if (logged.isEmpty()) "-" else String.format("%.1f", logged.average())
    }

    val activeDays = dayList.indices.count { i ->
        tasksDone[i] + habitsDone[i] + focusMin[i] + studyMin[i] + workoutMin[i] > 0f
    }
    val activeFraction = if (days == 0) 0f else activeDays.toFloat() / days

    val expenses = finance.filter { it.kind == "Expense" }
    val spentPaise = expenses.sumOf { it.amountPaise }
    val byCategory = expenses.groupBy { it.category }
        .map { it.key to it.value.sumOf { e -> e.amountPaise } }
        .sortedByDescending { it.second }
        .take(4)
    val palette = listOf(c.accent, ChartPurple, ChartGreen, ChartBlue)

    val names = listOf("Tasks", "Focus", "Mood", "Spent")
    val series = listOf(tasksDone, focusMin, moodFilled, spent)
    val colors = listOf(c.accent, ChartPurple, ChartGreen, ChartBlue)
    val fmts: List<(Float) -> String> = listOf(
        { v -> v.toInt().toString() + " done" },
        { v -> fmtMin(v.toInt()) },
        { v -> String.format("%.1f", v) + "/5" },
        { v -> formatRupees((v * 100).toLong()) }
    )
    val m = metric.coerceIn(0, 3)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Report",
                color = c.textPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniChip(label = "7 days", selected = rangeIndex == 0, onClick = { rangeIndex = 0 })
                MiniChip(label = "30 days", selected = rangeIndex == 1, onClick = { rangeIndex = 1 })
            }
        }

        // Orange summary card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFFF7A2F), Color(0xFFFF9F5A))))
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ACTIVE DAYS",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = activeDays.toString(),
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = " / " + days,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Text(
                    text = "Focus avg " + fmtMin(focusMin.average().toInt()) + " per day",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
            }
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(72.dp)) {
                    val sw = 9.dp.toPx()
                    val inset = sw / 2f
                    val arcSize = Size(this.size.width - sw, this.size.height - sw)
                    drawArc(
                        color = Color.White.copy(alpha = 0.3f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = sw)
                    )
                    if (activeFraction > 0f) {
                        drawArc(
                            color = Color.White,
                            startAngle = -90f,
                            sweepAngle = 360f * activeFraction,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = sw, cap = StrokeCap.Round)
                        )
                    }
                }
                Text(
                    text = (activeFraction * 100).toInt().toString() + "%",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // 2x2 tiles with sparklines
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReportTile(
                "TASKS", tasksDone.sum().toInt().toString(), colors[0], series[0],
                m == 0, { metric = 0 }, Modifier.weight(1f)
            )
            ReportTile(
                "FOCUS", fmtMin(focusMin.sum().toInt()), colors[1], series[1],
                m == 1, { metric = 1 }, Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReportTile(
                "MOOD", avg(mood), colors[2], series[2],
                m == 2, { metric = 2 }, Modifier.weight(1f)
            )
            ReportTile(
                "SPENT", formatRupees(spentPaise), colors[3], series[3],
                m == 3, { metric = 3 }, Modifier.weight(1f)
            )
        }

        // Insight chart
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    names.forEachIndexed { i, n ->
                        MiniChip(label = n, selected = m == i, onClick = { metric = i })
                    }
                }
                InsightChart(series[m], labels, dates, colors[m], fmts[m])
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            ChartBlock("Tasks done", tasksDone.sum().toInt().toString() + " total") {
                ValueBars(tasksDone, labels, showValues = showValues, height = 80.dp)
            }
        }

        // Spending
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "SPENDING", color = c.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = formatRupees(spentPaise),
                    color = c.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(c.track)
                ) {
                    byCategory.forEachIndexed { i, pair ->
                        Box(
                            modifier = Modifier
                                .weight(pair.second.toFloat().coerceAtLeast(1f))
                                .height(8.dp)
                                .background(palette[i % palette.size])
                        )
                    }
                }
                if (byCategory.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        byCategory.take(3).forEachIndexed { i, pair ->
                            LegendDotLabel(palette[i % palette.size], pair.first + " " + formatRupees(pair.second))
                        }
                    }
                }
            }
        }

        // More details (hidden by default)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            MiniChip(
                label = if (more == 0) "More details" else "Hide details",
                selected = false,
                onClick = { more = if (more == 0) 1 else 0 }
            )
        }
        if (more == 1) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (habitList.isNotEmpty()) {
                        ChartBlock("Habit ticks", habitsDone.sum().toInt().toString() + " total") {
                            ValueBars(habitsDone, labels, showValues = showValues, color = ChartGreen, height = 80.dp)
                        }
                    }
                    ChartBlock("Study and coding (min)", studyMin.sum().toInt().toString() + " total") {
                        ValueBars(studyMin, labels, showValues = showValues, color = ChartBlue, height = 80.dp)
                    }
                    ChartBlock("Workout (min)", workoutMin.sum().toInt().toString() + " total") {
                        ValueBars(workoutMin, labels, showValues = showValues, color = ChartRose, height = 80.dp)
                    }
                    ChartBlock("Water (glasses)", "avg " + String.format("%.1f", water.average())) {
                        ValueBars(water, labels, showValues = showValues, color = ChartBlue, height = 80.dp)
                    }
                    ChartBlock("Sleep (hours)", "avg " + avg(sleepHours)) {
                        ValueBars(
                            sleepHours, labels, showValues = showValues, color = ChartPurple, height = 80.dp,
                            valueText = { String.format("%.1f", it) }
                        )
                    }
                    ChartBlock("Energy", "avg " + avg(energy)) {
                        ValueBars(energy, labels, showValues = showValues, color = ChartPurple, height = 80.dp)
                    }
                    if (received.any { it > 0f }) {
                        ChartBlock("Money received", formatRupees((received.sum() * 100).toLong())) {
                            ValueBars(
                                received, labels, showValues = showValues, color = ChartGreen, height = 80.dp,
                                valueText = { it.toInt().toString() }
                            )
                        }
                    }
                }
            }
        }
    }
}

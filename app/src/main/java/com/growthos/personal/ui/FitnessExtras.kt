package com.growthos.personal.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.ExerciseLogEntity
import com.growthos.personal.data.MeasurementEntity
import com.growthos.personal.data.WorkoutPlanEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private fun kgText(v: Double): String =
    if (v == Math.floor(v)) v.toLong().toString() else String.format(Locale.US, "%.1f", v)

private fun dayLabel(epochDay: Long, today: Long): String =
    if (epochDay == today) "Today" else LocalDate.ofEpochDay(epochDay)
        .format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH))

private fun decimalOnly(text: String): String = text.filter { it.isDigit() || it == '.' }.take(6)

// ---------------------------------------------------------------------------
// Sets x reps @ kg
// ---------------------------------------------------------------------------

@Composable
fun LiftsPane() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).exerciseDao() }
    val today = remember { LocalDate.now().toEpochDay() }
    val flow = remember(dao) { dao.observeSince(today - 59) }
    val logs by flow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var name by rememberSaveable { mutableStateOf("") }
    var sets by rememberSaveable { mutableStateOf("") }
    var reps by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val recentNames = logs.map { it.exercise }.distinct().take(6)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TileLabel("Log sets")
                if (recentNames.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        recentNames.forEach { n ->
                            MiniChip(label = n, selected = name == n, onClick = { name = n })
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(30)
                        errorText = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Exercise") },
                    isError = errorText != null,
                    supportingText = {
                        val message = errorText
                        if (message != null) Text(message)
                    },
                    colors = glassFieldColors()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sets,
                        onValueChange = { sets = it.filter { ch -> ch.isDigit() }.take(2) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Sets") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = glassFieldColors()
                    )
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it.filter { ch -> ch.isDigit() }.take(3) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = glassFieldColors()
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = decimalOnly(it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = glassFieldColors()
                    )
                }
                GlowButton(
                    text = "Log exercise",
                    onClick = {
                        val n = name.trim()
                        val s = sets.toIntOrNull()
                        val r = reps.toIntOrNull()
                        val w = if (weight.isEmpty()) 0.0 else weight.toDoubleOrNull()
                        if (n.isEmpty()) {
                            errorText = "Enter the exercise name"
                        } else if (s == null || s < 1 || s > 50) {
                            errorText = "Sets: 1 to 50"
                        } else if (r == null || r < 1 || r > 500) {
                            errorText = "Reps: 1 to 500"
                        } else if (w == null || w < 0.0 || w > 500.0) {
                            errorText = "Weight: 0 to 500 kg"
                        } else {
                            scope.launch {
                                dao.insert(
                                    ExerciseLogEntity(
                                        epochDay = today,
                                        exercise = n,
                                        sets = s,
                                        reps = r,
                                        weightKg = w
                                    )
                                )
                            }
                            sets = ""
                            reps = ""
                            weight = ""
                            errorText = null
                        }
                    }
                )
            }
        }

        if (logs.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TileLabel("Recent")
                    logs.take(10).forEach { log ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dayLabel(log.epochDay, today),
                                    color = c.accent,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = log.exercise,
                                    color = c.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "${log.sets} x ${log.reps}" +
                                    (if (log.weightKg > 0.0) " @ " + kgText(log.weightKg) + " kg" else ""),
                                color = c.textSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            MoreMenu(listOf("Remove" to { scope.launch { dao.delete(log.id) } }))
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Weekly workout plan
// ---------------------------------------------------------------------------

private val WEEK_DAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

@Composable
fun PlanPane() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).workoutPlanDao() }
    val flow = remember(dao) { dao.observeAll() }
    val plan by flow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val todayDow = remember { LocalDate.now().dayOfWeek.value }

    var editingDay by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TileLabel("Weekly plan")
                WEEK_DAYS.forEachIndexed { index, dayName ->
                    val dow = index + 1
                    val entry = plan.firstOrNull { it.dayOfWeek == dow }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clickNoRipple { editingDay = dow },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dayName,
                            color = if (dow == todayDow) c.accent else c.textSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(104.dp)
                        )
                        Text(
                            text = entry?.name ?: "Not set",
                            color = if (entry == null) c.textTertiary else c.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        MoreMenu(
                            listOf(
                                (if (entry == null) "Set" else "Edit") to { editingDay = dow },
                                "Clear" to { scope.launch { dao.delete(dow) } }
                            )
                        )
                    }
                }
            }
        }
    }

    if (editingDay != 0) {
        val dow = editingDay
        TextEditDialog(
            heading = WEEK_DAYS[dow - 1] + " plan",
            initial = plan.firstOrNull { it.dayOfWeek == dow }?.name ?: "",
            maxLength = 30,
            onSave = { text ->
                scope.launch { dao.upsert(WorkoutPlanEntity(dayOfWeek = dow, name = text)) }
                editingDay = 0
            },
            onDismiss = { editingDay = 0 }
        )
    }
}

// ---------------------------------------------------------------------------
// Body measurements
// ---------------------------------------------------------------------------

private val BODY_KINDS = listOf("Weight", "Waist", "Chest", "Arms")

private fun bodyUnit(kind: String): String = if (kind == "Weight") "kg" else "cm"

@Composable
fun BodyPane() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).measurementDao() }
    val today = remember { LocalDate.now().toEpochDay() }
    val flow = remember(dao) { dao.observeAll() }
    val all by flow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var kind by rememberSaveable { mutableStateOf("Weight") }
    var value by rememberSaveable { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val list = all.filter { it.kind == kind }
    val latest = list.firstOrNull()
    val previous = list.getOrNull(1)
    val unit = bodyUnit(kind)
    val change = if (latest != null && previous != null) latest.value - previous.value else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = "Latest $kind",
                value = if (latest == null) "-" else kgText(latest.value),
                unit = if (latest == null) "" else unit,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Change",
                value = if (change == null) "-" else (if (change > 0) "+" else "") + String.format(Locale.US, "%.1f", change),
                unit = if (change == null) "" else unit,
                modifier = Modifier.weight(1f)
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TileLabel("Log a measurement")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BODY_KINDS.forEach { k ->
                        MiniChip(label = k, selected = kind == k, onClick = {
                            kind = k
                            errorText = null
                        })
                    }
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = decimalOnly(it)
                        errorText = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("$kind ($unit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = errorText != null,
                    supportingText = {
                        val message = errorText
                        if (message != null) Text(message)
                    },
                    colors = glassFieldColors()
                )
                GlowButton(
                    text = "Log $kind",
                    onClick = {
                        val v = value.toDoubleOrNull()
                        if (v == null || v <= 0.0 || v > 500.0) {
                            errorText = "Enter a value from 1 to 500"
                        } else {
                            scope.launch { dao.insert(MeasurementEntity(epochDay = today, kind = kind, value = v)) }
                            value = ""
                            errorText = null
                        }
                    }
                )
            }
        }

        if (list.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TileLabel("History")
                    list.take(8).forEach { m ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dayLabel(m.epochDay, today),
                                color = c.accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = kgText(m.value) + " " + unit,
                                color = c.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            MoreMenu(listOf("Remove" to { scope.launch { dao.delete(m.id) } }))
                        }
                    }
                }
            }
        }
    }
}

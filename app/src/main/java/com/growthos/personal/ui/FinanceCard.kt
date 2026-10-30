package com.growthos.personal.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.FinanceEntryEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val EXPENSE_CATEGORIES = listOf("Food", "Travel", "Study", "Bills", "Shopping", "Health", "Fun", "Other")
private val INCOME_CATEGORIES = listOf("Pocket money", "Salary", "Gift", "Other")
private val CATEGORY_COLORS = listOf(
    Color(0xFFFF7A2F), Color(0xFF6C63FF), Color(0xFF2EC4B6), Color(0xFFFF5C8A),
    Color(0xFFFFB400), Color(0xFF3A86FF), Color(0xFF8AC926), Color(0xFF9B5DE5)
)
private val GREEN = Color(0xFF2EC4B6)
private val ROSE = Color(0xFFFF5C8A)

private fun categoryEmoji(category: String): String = when (category) {
    "Food" -> "\uD83C\uDF54"
    "Travel" -> "\uD83D\uDE8C"
    "Study" -> "\uD83D\uDCDA"
    "Bills" -> "\uD83E\uDDFE"
    "Shopping" -> "\uD83D\uDECD\uFE0F"
    "Health" -> "\uD83D\uDC8A"
    "Fun" -> "\uD83C\uDFAE"
    "Pocket money" -> "\uD83D\uDC5B"
    "Salary" -> "\uD83D\uDCBC"
    "Gift" -> "\uD83C\uDF81"
    "Other" -> "\uD83D\uDCE6"
    else -> "\uD83D\uDCB0"
}

private fun categoryColor(category: String): Color {
    val i = EXPENSE_CATEGORIES.indexOf(category)
    return if (i >= 0) CATEGORY_COLORS[i % CATEGORY_COLORS.size] else GREEN
}

fun formatRupees(paise: Long): String {
    val sign = if (paise < 0) "-" else ""
    val a = if (paise < 0) -paise else paise
    val rupees = a / 100
    val rest = a % 100
    return if (rest == 0L) {
        sign + "\u20B9" + rupees
    } else {
        sign + "\u20B9" + rupees + "." + rest.toString().padStart(2, '0')
    }
}

private fun parsePaise(text: String): Long? {
    if (!Regex("^\\d{1,7}(\\.\\d{1,2})?$").matches(text)) return null
    val parts = text.split(".")
    val whole = parts[0].toLong()
    val frac = if (parts.size > 1) parts[1].padEnd(2, '0').toLong() else 0L
    val total = whole * 100 + frac
    return if (total <= 0L) null else total
}

private fun paiseToInput(paise: Long): String {
    val rupees = paise / 100
    val rest = paise % 100
    return if (rest == 0L) rupees.toString() else rupees.toString() + "." + rest.toString().padStart(2, '0')
}

@Composable
fun FinanceCard() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).financeDao() }
    val scope = rememberCoroutineScope()
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FinanceEntryEntity?>(null) }
    var deleting by remember { mutableStateOf<FinanceEntryEntity?>(null) }

    val month = YearMonth.now().plusMonths(monthOffset.toLong())
    val fromDay = month.atDay(1).toEpochDay()
    val toDay = month.atEndOfMonth().toEpochDay()
    val flow = remember(dao, monthOffset) { dao.observeBetween(fromDay, toDay) }
    val entries by flow.collectAsState(initial = emptyList())
    val today = remember { LocalDate.now().toEpochDay() }

    val expenses = entries.filter { it.kind == "Expense" }
    val incomes = entries.filter { it.kind != "Expense" }
    val spent = expenses.sumOf { it.amountPaise }
    val received = incomes.sumOf { it.amountPaise }
    val balance = received - spent
    val byCategory = expenses.groupBy { it.category }
        .map { it.key to it.value.sumOf { e -> e.amountPaise } }
        .sortedByDescending { it.second }
    val shown = entries.filter {
        when (filter) {
            1 -> it.kind == "Expense"
            2 -> it.kind != "Expense"
            else -> true
        }
    }.sortedWith(compareByDescending<FinanceEntryEntity> { it.epochDay }.thenByDescending { it.id })
    val grouped = shown.groupBy { it.epochDay }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // month switcher
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(c.tag)
                    .clickNoRipple { monthOffset -= 1 },
                contentAlignment = Alignment.Center
            ) { Text("<", color = c.accent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) }
            Text(
                text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)),
                color = c.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(c.tag)
                    .clickNoRipple { if (monthOffset < 0) monthOffset += 1 },
                contentAlignment = Alignment.Center
            ) { Text(">", color = if (monthOffset < 0) c.accent else c.textTertiary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) }
        }

        // balance hero
        GlassCard(modifier = Modifier.fillMaxWidth(), glow = true) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Balance this month", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = formatRupees(balance),
                    color = if (balance < 0) ROSE else c.textPrimary,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                            .background(GREEN.copy(alpha = 0.16f)).padding(12.dp)
                    ) {
                        Text("Received", color = c.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(formatRupees(received), color = GREEN, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                            .background(ROSE.copy(alpha = 0.16f)).padding(12.dp)
                    ) {
                        Text("Spent", color = c.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(formatRupees(spent), color = ROSE, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                if (received > 0L) {
                    val used = (spent.toFloat() / received.toFloat())
                    ThinBar(progress = used.coerceIn(0f, 1f))
                    Text(
                        text = "You spent " + (used * 100).toInt() + "% of what you received",
                        color = c.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // category breakdown
        if (byCategory.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Where your money went", color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DonutChart(
                            slices = byCategory.map { it.second.toFloat() to categoryColor(it.first) },
                            diameter = 130.dp,
                            stroke = 20.dp,
                            center = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Spent", color = c.textSecondary, fontSize = 11.sp)
                                    Text(formatRupees(spent), color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            byCategory.take(5).forEach { pair ->
                                val pct = if (spent > 0) (pair.second * 100 / spent).toInt() else 0
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(categoryColor(pair.first)))
                                    Text(
                                        text = " " + pair.first,
                                        color = c.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = pct.toString() + "%",
                                        color = c.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    byCategory.forEach { pair ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(categoryEmoji(pair.first), fontSize = 18.sp)
                            Text(
                                text = "  " + pair.first,
                                color = c.textPrimary,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(formatRupees(pair.second), color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // spending by day
            val days = month.lengthOfMonth()
            val perDay = (1..days).map { d ->
                val epoch = month.atDay(d).toEpochDay()
                expenses.filter { it.epochDay == epoch }.sumOf { it.amountPaise } / 100f
            }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Spending by day", color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    ValueBars(
                        values = perDay,
                        labels = (1..days).map { if (it % 5 == 0 || it == 1) it.toString() else "" },
                        showValues = false,
                        color = ROSE,
                        height = 90.dp
                    )
                    val topDay = perDay.indices.maxByOrNull { perDay[it] } ?: 0
                    if (perDay[topDay] > 0f) {
                        Text(
                            text = "Highest: " + formatRupees((perDay[topDay] * 100).toLong()) + " on " + (topDay + 1) + " " +
                                month.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)),
                            color = c.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // transactions header
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Transactions", color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            AddButton(onClick = { showAdd = true }, diameter = 46.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniChip(label = "All", selected = filter == 0, onClick = { filter = 0 })
            MiniChip(label = "Spent", selected = filter == 1, onClick = { filter = 1 })
            MiniChip(label = "Received", selected = filter == 2, onClick = { filter = 2 })
        }

        if (shown.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No entries yet. Tap + to add what you spent or received.",
                    color = c.textSecondary,
                    fontSize = 14.sp
                )
            }
        }
        grouped.keys.sortedDescending().forEach { day ->
            val list = grouped[day] ?: emptyList()
            val dayNet = list.sumOf { if (it.kind == "Expense") -it.amountPaise else it.amountPaise }
            val dayName = when (day) {
                today -> "Today"
                today - 1 -> "Yesterday"
                else -> LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(dayName, color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    text = (if (dayNet >= 0) "+" else "") + formatRupees(dayNet),
                    color = if (dayNet >= 0) GREEN else ROSE,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 12.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    list.forEach { e ->
                        val isExpense = e.kind == "Expense"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(CircleShape)
                                    .background((if (isExpense) categoryColor(e.category) else GREEN).copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) { Text(categoryEmoji(e.category), fontSize = 19.sp) }
                            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(
                                    text = if (e.note.isNotBlank()) e.note else e.category,
                                    color = c.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(e.category, color = c.textSecondary, fontSize = 12.sp, maxLines = 1)
                            }
                            Text(
                                text = (if (isExpense) "-" else "+") + formatRupees(e.amountPaise),
                                color = if (isExpense) ROSE else GREEN,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            MoreMenu(
                                listOf(
                                    "Edit" to { editing = e },
                                    "Delete" to { deleting = e }
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        EntryDialog(
            initial = null,
            onDismiss = { showAdd = false },
            onSave = { entry ->
                scope.launch { dao.insert(entry) }
                showAdd = false
            }
        )
    }
    val current = editing
    if (current != null) {
        EntryDialog(
            initial = current,
            onDismiss = { editing = null },
            onSave = { entry ->
                scope.launch {
                    dao.updateFull(current.id, entry.epochDay, entry.kind, entry.amountPaise, entry.category, entry.note)
                }
                editing = null
            }
        )
    }
    val toDelete = deleting
    if (toDelete != null) {
        ThemedDialog(
            title = "Delete this entry?",
            onDismiss = { deleting = null },
            content = {
                Text(
                    text = formatRupees(toDelete.amountPaise) + "  " + toDelete.category,
                    color = c.textSecondary,
                    fontSize = 15.sp
                )
            },
            actions = {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { deleting = null }) { Text("Cancel", color = c.textSecondary) }
                GlowButton(
                    text = "Delete",
                    onClick = {
                        deleting = null
                        scope.launch { dao.delete(toDelete.id) }
                    }
                )
            }
        )
    }
}

@Composable
private fun EntryDialog(
    initial: FinanceEntryEntity?,
    onDismiss: () -> Unit,
    onSave: (FinanceEntryEntity) -> Unit
) {
    val c = Growth.colors
    val context = LocalContext.current
    var isExpense by remember { mutableStateOf(initial?.kind != "Income") }
    var amount by remember { mutableStateOf(if (initial != null) paiseToInput(initial.amountPaise) else "") }
    var category by remember { mutableStateOf(initial?.category ?: "Food") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var day by remember { mutableStateOf(initial?.epochDay ?: LocalDate.now().toEpochDay()) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val categories = if (isExpense) EXPENSE_CATEGORIES else INCOME_CATEGORIES

    ThemedDialog(
        title = if (initial == null) "Add entry" else "Edit entry",
        onDismiss = onDismiss,
        content = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniChip(
                    label = "Spent", selected = isExpense,
                    onClick = {
                        isExpense = true
                        category = "Food"
                    },
                    modifier = Modifier.weight(1f)
                )
                MiniChip(
                    label = "Received", selected = !isExpense,
                    onClick = {
                        isExpense = false
                        category = "Pocket money"
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it
                    errorText = null
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Amount (\u20B9)") },
                isError = errorText != null,
                supportingText = {
                    val message = errorText
                    if (message != null) Text(message)
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                ),
                colors = glassFieldColors()
            )
            Text("Category", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            categories.chunked(3).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems.forEach { cat ->
                        MiniChip(
                            label = categoryEmoji(cat) + " " + cat,
                            selected = category == cat,
                            onClick = { category = cat },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.tag)
                    .clickNoRipple {
                        val d = LocalDate.ofEpochDay(day)
                        DatePickerDialog(
                            context,
                            { _, y, m, dd -> day = LocalDate.of(y, m + 1, dd).toEpochDay() },
                            d.year, d.monthValue - 1, d.dayOfMonth
                        ).show()
                    }
                    .padding(horizontal = 16.dp, vertical = 13.dp)
            ) {
                Text(
                    text = "Date: " + LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)),
                    color = c.accent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 60) note = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Note (optional)") },
                colors = glassFieldColors()
            )
        },
        actions = {
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("Cancel", color = c.textSecondary) }
            GlowButton(
                text = "Save",
                onClick = {
                    val paise = parsePaise(amount.trim())
                    if (paise == null) {
                        errorText = "Enter an amount like 120 or 49.50"
                    } else {
                        onSave(
                            FinanceEntryEntity(
                                id = initial?.id ?: 0,
                                epochDay = day,
                                kind = if (isExpense) "Expense" else "Income",
                                amountPaise = paise,
                                category = category,
                                note = note.trim()
                            )
                        )
                    }
                }
            )
        }
    )
}

package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.FinanceEntryEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val EXPENSE_CATEGORIES = listOf("Food", "Travel", "Study", "Bills", "Other")

fun formatRupees(paise: Long): String {
    val sign = if (paise < 0) "-" else ""
    val a = if (paise < 0) -paise else paise
    val rupees = a / 100
    val rest = a % 100
    return if (rest == 0L) {
        sign + "₹" + rupees
    } else {
        sign + "₹" + rupees + "." + rest.toString().padStart(2, '0')
    }
}

private fun parsePaise(text: String): Long? {
    if (!Regex("^\\d{1,7}(\\.\\d{1,2})?\$").matches(text)) return null
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
private fun FinanceEditDialog(
    entry: FinanceEntryEntity,
    onSave: (Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf(paiseToInput(entry.amountPaise)) }
    var note by remember { mutableStateOf(entry.note) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10)
                        error = null
                    },
                    singleLine = true,
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = error != null,
                    supportingText = {
                        val message = error
                        if (message != null) Text(message)
                    }
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    singleLine = true,
                    label = { Text("Note (optional)") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val paise = parsePaise(amount.trim())
                val n = note.trim()
                if (paise == null) {
                    error = "Enter an amount like 250 or 99.50"
                } else if (n.length > 60) {
                    error = "Keep the note under 60 characters"
                } else {
                    onSave(paise, n)
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun FinanceCard() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).financeDao() }
    val todayDate = remember { LocalDate.now() }
    val today = todayDate.toEpochDay()
    val monthStart = todayDate.withDayOfMonth(1).toEpochDay()
    val monthEnd = todayDate.withDayOfMonth(todayDate.lengthOfMonth()).toEpochDay()
    val flow = remember(dao) { dao.observeBetween(monthStart, monthEnd) }
    val entries by flow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var kind by rememberSaveable { mutableStateOf("Expense") }
    var amount by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Food") }
    var note by rememberSaveable { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var editingEntry by remember { mutableStateOf<FinanceEntryEntity?>(null) }

    val income = entries.filter { it.kind == "Income" }.sumOf { it.amountPaise }
    val spent = entries.filter { it.kind == "Expense" }.sumOf { it.amountPaise }
    val fmt = remember { DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "Finance", color = c.textPrimary, fontSize = 18.sp)
            Text(
                text = "This month: Income " + formatRupees(income) + ", Spent " + formatRupees(spent),
                color = c.textPrimary,
                fontSize = 14.sp
            )
            Text(
                text = "Balance: " + formatRupees(income - spent),
                color = c.textSecondary,
                fontSize = 13.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LogChip(label = "Expense", selected = kind == "Expense", onClick = { kind = "Expense" })
                LogChip(label = "Income", selected = kind == "Income", onClick = { kind = "Income" })
            }
            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10)
                    errorText = null
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = errorText != null,
                supportingText = {
                    val message = errorText
                    if (message != null) Text(message)
                },
                colors = glassFieldColors()
            )
            if (kind == "Expense") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EXPENSE_CATEGORIES.forEach { cat ->
                        LogChip(label = cat, selected = category == cat, onClick = { category = cat })
                    }
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Note (optional)") },
                colors = glassFieldColors()
            )
            Button(
                onClick = {
                    val paise = parsePaise(amount.trim())
                    val n = note.trim()
                    if (paise == null) {
                        errorText = "Enter an amount like 250 or 99.50"
                    } else if (n.length > 60) {
                        errorText = "Keep the note under 60 characters"
                    } else {
                        scope.launch {
                            dao.insert(
                                FinanceEntryEntity(
                                    epochDay = today,
                                    kind = kind,
                                    amountPaise = paise,
                                    category = if (kind == "Income") "Income" else category,
                                    note = n
                                )
                            )
                        }
                        amount = ""
                        note = ""
                        errorText = null
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White)
            ) {
                Text("Add entry")
            }

            entries.take(8).forEach { e ->
                val sign = if (e.kind == "Income") "+" else "-"
                val dateText = LocalDate.ofEpochDay(e.epochDay).format(fmt)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateText + " | " + e.category + " | " + sign + formatRupees(e.amountPaise) +
                            if (e.note.isEmpty()) "" else " | " + e.note,
                        color = c.textPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { editingEntry = e }) {
                        Text("Edit", color = c.textSecondary)
                    }
                    TextButton(onClick = { scope.launch { dao.delete(e.id) } }) {
                        Text("Remove", color = c.textSecondary)
                    }
                }
            }
            if (entries.size > 8) {
                Text(
                    text = "Showing latest 8 of ${entries.size} entries this month",
                    color = c.textSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }

    val entryBeingEdited = editingEntry
    if (entryBeingEdited != null) {
        FinanceEditDialog(
            entry = entryBeingEdited,
            onSave = { newPaise, newNote ->
                scope.launch { dao.update(entryBeingEdited.id, newPaise, newNote) }
                editingEntry = null
            },
            onDismiss = { editingEntry = null }
        )
    }
}

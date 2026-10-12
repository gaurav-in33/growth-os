package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.NoteEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun NotesCard() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).noteDao() }
    val flow = remember(dao) { dao.observeLatest() }
    val notes by flow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val fmt = remember { DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH) }

    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var toDelete by remember { mutableStateOf<NoteEntity?>(null) }
    var editing by remember { mutableStateOf<NoteEntity?>(null) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "Notes", color = c.textPrimary, fontSize = 18.sp)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Title (optional)") },
                colors = glassFieldColors()
            )
            OutlinedTextField(
                value = body,
                onValueChange = {
                    body = it
                    errorText = null
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                label = { Text("Write a note") },
                isError = errorText != null,
                supportingText = {
                    val message = errorText
                    if (message != null) Text(message)
                },
                colors = glassFieldColors()
            )
            Button(
                onClick = {
                    val t = title.trim()
                    val b = body.trim()
                    if (b.isEmpty()) {
                        errorText = "Write something first"
                    } else if (b.length > 2000) {
                        errorText = "Keep the note under 2000 characters"
                    } else if (t.length > 60) {
                        errorText = "Keep the title under 60 characters"
                    } else {
                        scope.launch {
                            dao.insert(
                                NoteEntity(
                                    createdAtMillis = System.currentTimeMillis(),
                                    title = t,
                                    body = b
                                )
                            )
                        }
                        title = ""
                        body = ""
                        errorText = null
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White)
            ) {
                Text("Save note")
            }

            if (notes.isEmpty()) {
                Text(text = "No notes yet.", color = c.textSecondary, fontSize = 14.sp)
            }

            notes.take(10).forEach { n ->
                val stamp = Instant.ofEpochMilli(n.createdAtMillis)
                    .atZone(ZoneId.systemDefault())
                    .format(fmt)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (n.title.isEmpty()) "Note" else n.title,
                                color = c.textPrimary,
                                fontSize = 15.sp
                            )
                            Text(text = stamp, color = c.textSecondary, fontSize = 12.sp)
                        }
                        TextButton(onClick = { editing = n }) {
                            Text("Edit", color = c.textSecondary)
                        }
                        TextButton(onClick = { toDelete = n }) {
                            Text("Remove", color = c.textSecondary)
                        }
                    }
                    Text(text = n.body, color = c.textPrimary, fontSize = 14.sp)
                }
            }
            if (notes.size > 10) {
                Text(
                    text = "Showing latest 10 of ${notes.size} notes",
                    color = c.textSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }

    val noteBeingEdited = editing
    if (noteBeingEdited != null) {
        NoteEditDialog(
            initialTitle = noteBeingEdited.title,
            initialBody = noteBeingEdited.body,
            onSave = { newTitle, newBody ->
                scope.launch { dao.update(noteBeingEdited.id, newTitle, newBody) }
                editing = null
            },
            onDismiss = { editing = null }
        )
    }

    val pending = toDelete
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete note?") },
            text = { Text("This note will be removed for good.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.delete(pending.id) }
                    toDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Cancel") }
            }
        )
    }
}

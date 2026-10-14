package com.growthos.personal.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

@Composable
fun TextEditDialog(
    heading: String,
    initial: String,
    maxLength: Int,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    extraCheck: (String) -> String? = { null }
) {
    val c = Growth.colors
    var text by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.solid,
        titleContentColor = c.textPrimary,
        textContentColor = c.textPrimary,
        title = { Text(heading) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    error = null
                },
                singleLine = true,
                isError = error != null,
                supportingText = {
                    val message = error
                    if (message != null) Text(message)
                },
                colors = glassFieldColors()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val t = text.trim()
                val problem = extraCheck(t)
                if (t.isEmpty()) {
                    error = "This cannot be empty"
                } else if (t.length > maxLength) {
                    error = "Keep it under $maxLength characters"
                } else if (problem != null) {
                    error = problem
                } else {
                    onSave(t)
                }
            }) { Text("Save", color = c.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = c.textSecondary) }
        }
    )
}

@Composable
fun NoteEditDialog(
    initialTitle: String,
    initialBody: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val c = Growth.colors
    var title by remember { mutableStateOf(initialTitle) }
    var body by remember { mutableStateOf(initialBody) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.solid,
        titleContentColor = c.textPrimary,
        textContentColor = c.textPrimary,
        title = { Text("Edit note") },
        text = {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    label = { Text("Title (optional)") },
                    colors = glassFieldColors()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = {
                        body = it
                        error = null
                    },
                    minLines = 3,
                    maxLines = 6,
                    label = { Text("Note") },
                    isError = error != null,
                    supportingText = {
                        val message = error
                        if (message != null) Text(message)
                    },
                    colors = glassFieldColors()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val t = title.trim()
                val b = body.trim()
                if (b.isEmpty()) {
                    error = "Write something first"
                } else if (b.length > 2000) {
                    error = "Keep the note under 2000 characters"
                } else if (t.length > 60) {
                    error = "Keep the title under 60 characters"
                } else {
                    onSave(t, b)
                }
            }) { Text("Save", color = c.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = c.textSecondary) }
        }
    )
}

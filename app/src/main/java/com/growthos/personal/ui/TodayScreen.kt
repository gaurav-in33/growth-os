package com.growthos.personal.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.growthos.personal.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.TaskEntity
import kotlinx.coroutines.launch

@Composable
fun TodayScreen(name: String) {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).taskDao() }
    val tasksFlow = remember(dao) { dao.observeAll() }
    val tasks by tasksFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var newTitle by rememberSaveable { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }

    val doneCount = tasks.count { it.isDone }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val hour = LocalTime.now().hour
            val greeting = when {
                hour < 12 -> "Good morning"
                hour < 17 -> "Good afternoon"
                else -> "Good evening"
            }
            val dateText = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.profile_photo),
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                )
                Column {
                    Text(
                        text = if (name.isBlank()) greeting else "$greeting, $name",
                        color = c.textPrimary,
                        fontSize = 20.sp
                    )
                    Text(text = dateText, color = c.textSecondary, fontSize = 14.sp)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Today's Command Center",
                    color = c.textPrimary,
                    fontSize = 26.sp
                )
                Text(
                    text = "$doneCount of ${tasks.size} tasks done",
                    color = c.textSecondary,
                    fontSize = 14.sp
                )
            }
        }

        item { WakeCard() }
        item { FocusCard() }
        item { TodayHabitsCard() }
        item { WaterCard() }
        item { CheckinCard() }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = {
                            newTitle = it
                            errorText = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("What needs to be done?") },
                        isError = errorText != null,
                        supportingText = {
                            val message = errorText
                            if (message != null) Text(message)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = c.textPrimary,
                            unfocusedTextColor = c.textPrimary,
                            focusedBorderColor = c.accent,
                            unfocusedBorderColor = c.glassBorder,
                            cursorColor = c.accent,
                            focusedPlaceholderColor = c.textSecondary,
                            unfocusedPlaceholderColor = c.textSecondary
                        )
                    )
                    Button(
                        onClick = {
                            val title = newTitle.trim()
                            if (title.isEmpty()) {
                                errorText = "Enter a task name"
                            } else if (title.length > 120) {
                                errorText = "Keep it under 120 characters"
                            } else {
                                scope.launch {
                                    dao.insert(
                                        TaskEntity(
                                            title = title,
                                            createdAtMillis = System.currentTimeMillis()
                                        )
                                    )
                                }
                                newTitle = ""
                                errorText = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = c.accent,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Add task")
                    }
                }
            }
        }

        if (tasks.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No tasks yet. Add your first task above.",
                        color = c.textSecondary,
                        fontSize = 15.sp
                    )
                }
            }
        }

        items(tasks, key = { it.id }) { task ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = task.isDone,
                        onCheckedChange = { checked ->
                            scope.launch { dao.setDone(task.id, checked) }
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = c.accent,
                            uncheckedColor = c.textSecondary,
                            checkmarkColor = Color.White
                        )
                    )
                    Text(
                        text = task.title,
                        color = if (task.isDone) c.textSecondary else c.textPrimary,
                        fontSize = 16.sp,
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { editingTask = task }) {
                        Text("Edit", color = c.textSecondary)
                    }
                    IconButton(onClick = {
                        scope.launch { dao.delete(task.id) }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete task",
                            tint = c.textSecondary
                        )
                    }
                }
            }
        }
    }

    val taskBeingEdited = editingTask
    if (taskBeingEdited != null) {
        TextEditDialog(
            heading = "Edit task",
            initial = taskBeingEdited.title,
            maxLength = 120,
            onSave = { newTitle ->
                scope.launch { dao.rename(taskBeingEdited.id, newTitle) }
                editingTask = null
            },
            onDismiss = { editingTask = null }
        )
    }
}

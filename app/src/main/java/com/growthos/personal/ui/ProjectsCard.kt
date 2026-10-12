package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.growthos.personal.data.ProjectEntity
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun ProjectsCard() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).projectDao() }
    val flow = remember(dao) { dao.observeActive() }
    val projects by flow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now().toEpochDay() }

    var name by rememberSaveable { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var editingProject by remember { mutableStateOf<ProjectEntity?>(null) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "Projects", color = c.textPrimary, fontSize = 18.sp)
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorText = null
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("e.g. Portfolio website") },
                isError = errorText != null,
                supportingText = {
                    val message = errorText
                    if (message != null) Text(message)
                },
                colors = glassFieldColors()
            )
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        errorText = "Enter a project name"
                    } else if (trimmed.length > 60) {
                        errorText = "Keep it under 60 characters"
                    } else {
                        scope.launch {
                            dao.insert(
                                ProjectEntity(
                                    name = trimmed,
                                    progress = 0,
                                    createdEpochDay = today
                                )
                            )
                        }
                        name = ""
                        errorText = null
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White)
            ) {
                Text("Add project")
            }

            if (projects.isEmpty()) {
                Text(
                    text = "No projects yet. You set progress yourself by sliding it.",
                    color = c.textSecondary,
                    fontSize = 14.sp
                )
            }

            projects.forEach { project ->
                var draft by remember(project.id, project.progress) {
                    mutableFloatStateOf(project.progress.toFloat())
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = project.name, color = c.textPrimary, fontSize = 16.sp)
                            Text(
                                text = if (draft.roundToInt() >= 100) "Completed" else "${draft.roundToInt()}% done",
                                color = c.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                        TextButton(onClick = { editingProject = project }) {
                            Text("Edit", color = c.textSecondary)
                        }
                        TextButton(onClick = { scope.launch { dao.archive(project.id) } }) {
                            Text("Archive", color = c.textSecondary)
                        }
                    }
                    Slider(
                        value = draft,
                        onValueChange = { draft = it },
                        onValueChangeFinished = {
                            scope.launch { dao.setProgress(project.id, draft.roundToInt()) }
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = c.accent,
                            activeTrackColor = c.accent,
                            inactiveTrackColor = c.glassBorder
                        )
                    )
                }
            }
        }
    }

    val projectBeingEdited = editingProject
    if (projectBeingEdited != null) {
        TextEditDialog(
            heading = "Edit project",
            initial = projectBeingEdited.name,
            maxLength = 60,
            onSave = { newName ->
                scope.launch { dao.rename(projectBeingEdited.id, newName) }
                editingProject = null
            },
            onDismiss = { editingProject = null }
        )
    }
}

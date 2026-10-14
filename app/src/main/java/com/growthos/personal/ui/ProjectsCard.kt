package com.growthos.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.ProjectEntity
import java.time.LocalDate
import kotlinx.coroutines.launch

/** Grow > Projects. */
@Composable
fun ProjectsSection() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).projectDao() }
    val flow = remember(dao) { dao.observeActive() }
    val projects by flow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now().toEpochDay() }

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ProjectEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (projects.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "No projects yet", color = c.textSecondary, fontSize = 15.sp)
            }
        }
        projects.forEach { project ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RingProgress(progress = project.progress / 100f, diameter = 60.dp, stroke = 6.dp) {
                        Text(
                            text = "${project.progress}%",
                            color = c.textPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = project.name,
                            color = c.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (project.progress >= 100) "Completed" else "In progress",
                            color = c.textSecondary,
                            fontSize = 12.5.sp
                        )
                    }
                    StepButton("−") {
                        scope.launch { dao.setProgress(project.id, (project.progress - 10).coerceAtLeast(0)) }
                    }
                    StepButton("+") {
                        scope.launch { dao.setProgress(project.id, (project.progress + 10).coerceAtMost(100)) }
                    }
                    MoreMenu(
                        listOf(
                            "Edit" to { editing = project },
                            "Archive" to { scope.launch { dao.archive(project.id) } }
                        )
                    )
                }
            }
        }
        GlowButton(text = "New project", onClick = { showAdd = true })
    }

    if (showAdd) {
        TextEditDialog(
            heading = "New project",
            initial = "",
            maxLength = 60,
            onSave = { name ->
                scope.launch {
                    dao.insert(ProjectEntity(name = name, progress = 0, createdEpochDay = today))
                }
                showAdd = false
            },
            onDismiss = { showAdd = false }
        )
    }

    val projectBeingEdited = editing
    if (projectBeingEdited != null) {
        TextEditDialog(
            heading = "Edit project",
            initial = projectBeingEdited.name,
            maxLength = 60,
            onSave = { newName ->
                scope.launch { dao.rename(projectBeingEdited.id, newName) }
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
}

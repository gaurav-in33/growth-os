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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.AppDatabase
import com.growthos.personal.data.ProjectEntity
import com.growthos.personal.data.ProjectItemEntity
import com.growthos.personal.data.ProjectRules
import java.time.LocalDate
import kotlinx.coroutines.launch

/** Grow > Projects. Progress comes from milestones when a project has any. */
@Composable
fun ProjectsSection() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).projectDao() }
    val itemDao = remember { AppDatabase.get(context).projectItemDao() }
    val flow = remember(dao) { dao.observeActive() }
    val itemFlow = remember(itemDao) { itemDao.observeAll() }
    val projects by flow.collectAsState(initial = emptyList())
    val allItems by itemFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now().toEpochDay() }

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ProjectEntity?>(null) }
    var addMilestoneFor by remember { mutableStateOf<ProjectEntity?>(null) }
    var addSubtaskFor by remember { mutableStateOf<ProjectItemEntity?>(null) }
    var renamingItem by remember { mutableStateOf<ProjectItemEntity?>(null) }

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
            val mine = allItems.filter { it.projectId == project.id }
            val milestones = mine.filter { it.parentId == 0L }
            val auto = milestones.isNotEmpty()
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            val state = if (project.progress >= 100) "Completed" else "In progress"
                            Text(
                                text = if (auto) state + " · " + milestones.count { it.isDone } + "/" +
                                    milestones.size + " milestones" else state,
                                color = c.textSecondary,
                                fontSize = 12.5.sp
                            )
                        }
                        if (!auto) {
                            StepButton("−") {
                                scope.launch { dao.setProgress(project.id, (project.progress - 10).coerceAtLeast(0)) }
                            }
                            StepButton("+") {
                                scope.launch { dao.setProgress(project.id, (project.progress + 10).coerceAtMost(100)) }
                            }
                        }
                        MoreMenu(
                            listOf(
                                "Edit" to { editing = project },
                                "Add milestone" to { addMilestoneFor = project },
                                "Archive" to { scope.launch { dao.archive(project.id) } }
                            )
                        )
                    }

                    milestones.forEach { milestone ->
                        val subs = mine.filter { it.parentId == milestone.id }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CheckDot(
                                checked = milestone.isDone,
                                onClick = { scope.launch { ProjectRules.toggleMilestone(context, milestone) } }
                            )
                            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(
                                    text = milestone.title,
                                    color = if (milestone.isDone) c.textTertiary else c.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = if (milestone.isDone) TextDecoration.LineThrough else null
                                )
                                if (subs.isNotEmpty()) {
                                    Text(
                                        text = "${subs.count { it.isDone }}/${subs.size} subtasks",
                                        color = c.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            MoreMenu(
                                listOf(
                                    "Add subtask" to { addSubtaskFor = milestone },
                                    "Rename" to { renamingItem = milestone },
                                    "Delete" to { scope.launch { ProjectRules.delete(context, milestone) } }
                                )
                            )
                        }
                        subs.forEach { sub ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 30.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CheckDot(
                                    checked = sub.isDone,
                                    onClick = { scope.launch { ProjectRules.toggleSubtask(context, sub) } }
                                )
                                Text(
                                    text = sub.title,
                                    color = if (sub.isDone) c.textTertiary else c.textPrimary,
                                    fontSize = 14.sp,
                                    textDecoration = if (sub.isDone) TextDecoration.LineThrough else null,
                                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                                )
                                MoreMenu(
                                    listOf(
                                        "Rename" to { renamingItem = sub },
                                        "Delete" to { scope.launch { ProjectRules.delete(context, sub) } }
                                    )
                                )
                            }
                        }
                    }
                    Text(
                        text = "+ Add milestone",
                        color = c.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp).clickNoRipple { addMilestoneFor = project }
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

    val milestoneProject = addMilestoneFor
    if (milestoneProject != null) {
        TextEditDialog(
            heading = "New milestone",
            initial = "",
            maxLength = 80,
            onSave = { title ->
                scope.launch { ProjectRules.addMilestone(context, milestoneProject.id, title) }
                addMilestoneFor = null
            },
            onDismiss = { addMilestoneFor = null }
        )
    }

    val subtaskParent = addSubtaskFor
    if (subtaskParent != null) {
        TextEditDialog(
            heading = "New subtask",
            initial = "",
            maxLength = 80,
            onSave = { title ->
                scope.launch { ProjectRules.addSubtask(context, subtaskParent, title) }
                addSubtaskFor = null
            },
            onDismiss = { addSubtaskFor = null }
        )
    }

    val itemBeingRenamed = renamingItem
    if (itemBeingRenamed != null) {
        TextEditDialog(
            heading = "Rename",
            initial = itemBeingRenamed.title,
            maxLength = 80,
            onSave = { title ->
                scope.launch { itemDao.rename(itemBeingRenamed.id, title) }
                renamingItem = null
            },
            onDismiss = { renamingItem = null }
        )
    }
}

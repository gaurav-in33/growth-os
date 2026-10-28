package com.growthos.personal.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.growthos.personal.data.CourseEntity
import com.growthos.personal.data.SkillEntity
import com.growthos.personal.data.TopicEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val STATUS_LABELS = listOf("Not started", "Learning", "Done")

private fun deadlineText(daysLeft: Long): String = when {
    daysLeft < 0 -> "Overdue"
    daysLeft == 0L -> "Due today"
    daysLeft == 1L -> "1 day left"
    else -> "$daysLeft days left"
}

/** Grow > Learn > Courses. */
@Composable
fun CoursesPane() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).learnDao() }
    val coursesFlow = remember(dao) { dao.observeCourses() }
    val topicsFlow = remember(dao) { dao.observeTopics() }
    val courses by coursesFlow.collectAsState(initial = emptyList())
    val topics by topicsFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now().toEpochDay() }

    var showAdd by remember { mutableStateOf(false) }
    var editingCourse by remember { mutableStateOf<CourseEntity?>(null) }
    var addTopicFor by remember { mutableStateOf<CourseEntity?>(null) }
    var renamingTopic by remember { mutableStateOf<TopicEntity?>(null) }

    val courseIds = courses.map { it.id }.toSet()
    val weak = topics.filter { it.weak && it.courseId in courseIds }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (weak.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth(), glow = true) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TileLabel("Review weak areas", trailing = weak.size.toString())
                    weak.take(6).forEach { topic ->
                        val courseName = courses.firstOrNull { it.id == topic.courseId }?.name ?: ""
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = topic.name,
                                    color = c.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(text = courseName, color = c.textSecondary, fontSize = 12.sp)
                            }
                            Text(
                                text = "Reviewed",
                                color = c.accent,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickNoRipple {
                                    scope.launch { dao.setTopicWeak(topic.id, false) }
                                }
                            )
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (courses.isEmpty()) "No courses yet" else "${courses.size} courses",
                color = c.textSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(start = 2.dp)
            )
            AddButton(onClick = { showAdd = true }, diameter = 40.dp)
        }

        courses.forEach { course ->
            val list = topics.filter { it.courseId == course.id }
            val done = list.count { it.status == 2 }
            val deadline = course.deadlineEpochDay
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = course.name,
                                color = c.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$done of ${list.size} topics done",
                                color = c.textSecondary,
                                fontSize = 12.5.sp
                            )
                        }
                        if (deadline != null) {
                            TagPill(deadlineText(deadline - today))
                        }
                        MoreMenu(
                            listOf(
                                "Edit" to { editingCourse = course },
                                "Add topic" to { addTopicFor = course },
                                "Archive" to { scope.launch { dao.archiveCourse(course.id) } },
                                "Delete" to {
                                    scope.launch {
                                        dao.deleteTopicsOf(course.id)
                                        dao.deleteCourse(course.id)
                                    }
                                }
                            )
                        )
                    }
                    if (list.isNotEmpty()) {
                        ThinBar(progress = done.toFloat() / list.size)
                    }
                    list.forEach { topic ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickNoRipple {
                                        scope.launch { dao.setTopicStatus(topic.id, (topic.status + 1) % 3) }
                                    }
                            ) {
                                Text(
                                    text = topic.name,
                                    color = if (topic.status == 2) c.textTertiary else c.textPrimary,
                                    fontSize = 15.sp,
                                    textDecoration = if (topic.status == 2) TextDecoration.LineThrough else null
                                )
                                Text(
                                    text = STATUS_LABELS[topic.status.coerceIn(0, 2)],
                                    color = if (topic.status == 1) c.accent else c.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = if (topic.weak) "★" else "☆",
                                color = if (topic.weak) c.accent else c.textTertiary,
                                fontSize = 22.sp,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp)
                                    .clickNoRipple { scope.launch { dao.setTopicWeak(topic.id, !topic.weak) } }
                            )
                            MoreMenu(
                                listOf(
                                    "Rename" to { renamingTopic = topic },
                                    "Delete" to { scope.launch { dao.deleteTopic(topic.id) } }
                                )
                            )
                        }
                    }
                    Text(
                        text = "+ Add topic",
                        color = c.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp).clickNoRipple { addTopicFor = course }
                    )
                }
            }
        }
    }

    if (showAdd) {
        CourseEditDialog(
            initial = null,
            onDismiss = { showAdd = false },
            onSave = { name, deadline ->
                scope.launch { dao.insertCourse(CourseEntity(name = name, deadlineEpochDay = deadline)) }
                showAdd = false
            }
        )
    }
    val editCourse = editingCourse
    if (editCourse != null) {
        CourseEditDialog(
            initial = editCourse,
            onDismiss = { editingCourse = null },
            onSave = { name, deadline ->
                scope.launch { dao.updateCourse(editCourse.copy(name = name, deadlineEpochDay = deadline)) }
                editingCourse = null
            }
        )
    }
    val topicCourse = addTopicFor
    if (topicCourse != null) {
        TextEditDialog(
            heading = "New topic",
            initial = "",
            maxLength = 60,
            onSave = { name ->
                scope.launch { dao.insertTopic(TopicEntity(courseId = topicCourse.id, name = name)) }
                addTopicFor = null
            },
            onDismiss = { addTopicFor = null }
        )
    }
    val renameTopic = renamingTopic
    if (renameTopic != null) {
        TextEditDialog(
            heading = "Rename topic",
            initial = renameTopic.name,
            maxLength = 60,
            onSave = { name ->
                scope.launch { dao.renameTopic(renameTopic.id, name) }
                renamingTopic = null
            },
            onDismiss = { renamingTopic = null }
        )
    }
}

@Composable
private fun CourseEditDialog(
    initial: CourseEntity?,
    onDismiss: () -> Unit,
    onSave: (String, Long?) -> Unit
) {
    val c = Growth.colors
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var deadline by remember { mutableStateOf(initial?.deadlineEpochDay) }
    var error by remember { mutableStateOf<String?>(null) }
    val day = deadline

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.solid,
        titleContentColor = c.textPrimary,
        textContentColor = c.textPrimary,
        title = { Text(if (initial == null) "New course" else "Edit course") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    singleLine = true,
                    label = { Text("Course name") },
                    isError = error != null,
                    supportingText = {
                        val message = error
                        if (message != null) Text(message)
                    },
                    colors = glassFieldColors()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniChip(
                        label = if (day == null) "Set deadline" else LocalDate.ofEpochDay(day)
                            .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)),
                        selected = day != null,
                        onClick = {
                            val base = LocalDate.ofEpochDay(day ?: LocalDate.now().toEpochDay())
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> deadline = LocalDate.of(y, m + 1, d).toEpochDay() },
                                base.year,
                                base.monthValue - 1,
                                base.dayOfMonth
                            ).show()
                        }
                    )
                    if (day != null) {
                        Text(
                            text = "Clear",
                            color = c.accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickNoRipple { deadline = null }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val t = name.trim()
                if (t.isEmpty()) {
                    error = "Enter a course name"
                } else if (t.length > 60) {
                    error = "Keep it under 60 characters"
                } else {
                    onSave(t, deadline)
                }
            }) { Text("Save", color = c.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = c.textSecondary) }
        }
    )
}

/** Grow > Learn > Roadmap. */
@Composable
fun RoadmapPane() {
    val c = Growth.colors
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).learnDao() }
    val flow = remember(dao) { dao.observeSkills() }
    val skills by flow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var showAdd by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<SkillEntity?>(null) }

    val done = skills.count { it.status == 2 }

    fun move(index: Int, by: Int) {
        val target = index + by
        if (target < 0 || target >= skills.size) return
        val ordered = skills.toMutableList()
        val item = ordered.removeAt(index)
        ordered.add(target, item)
        scope.launch {
            ordered.forEachIndexed { i, s -> dao.setSkillPosition(s.id, i + 1) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (skills.isEmpty()) "No skills yet" else "$done of ${skills.size} skills done",
                color = c.textSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(start = 2.dp)
            )
            AddButton(onClick = { showAdd = true }, diameter = 40.dp)
        }

        if (skills.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TileLabel("Skill roadmap")
                    ThinBar(progress = done.toFloat() / skills.size)
                    skills.forEachIndexed { index, skill ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = c.textTertiary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickNoRipple {
                                        scope.launch { dao.setSkillStatus(skill.id, (skill.status + 1) % 3) }
                                    }
                            ) {
                                Text(
                                    text = skill.name,
                                    color = if (skill.status == 2) c.textTertiary else c.textPrimary,
                                    fontSize = 15.5.sp,
                                    textDecoration = if (skill.status == 2) TextDecoration.LineThrough else null
                                )
                                Text(
                                    text = listOf("To learn", "Learning", "Done")[skill.status.coerceIn(0, 2)],
                                    color = if (skill.status == 1) c.accent else c.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            MoreMenu(
                                listOf(
                                    "Move up" to { move(index, -1) },
                                    "Move down" to { move(index, 1) },
                                    "Rename" to { renaming = skill },
                                    "Delete" to { scope.launch { dao.deleteSkill(skill.id) } }
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        TextEditDialog(
            heading = "New skill",
            initial = "",
            maxLength = 50,
            onSave = { name ->
                scope.launch {
                    dao.insertSkill(SkillEntity(name = name, position = dao.maxSkillPosition() + 1))
                }
                showAdd = false
            },
            onDismiss = { showAdd = false }
        )
    }
    val renameSkill = renaming
    if (renameSkill != null) {
        TextEditDialog(
            heading = "Rename skill",
            initial = renameSkill.name,
            maxLength = 50,
            onSave = { name ->
                scope.launch { dao.renameSkill(renameSkill.id, name) }
                renaming = null
            },
            onDismiss = { renaming = null }
        )
    }
}

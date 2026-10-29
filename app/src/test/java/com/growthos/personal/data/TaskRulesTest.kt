package com.growthos.personal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskRulesTest {

    private val today = 1000L

    private fun task(
        id: Long = 1,
        isDone: Boolean = false,
        due: Long? = null,
        priority: Int = 2,
        completed: Long? = null
    ) = TaskEntity(
        id = id,
        title = "Task " + id,
        isDone = isDone,
        createdAtMillis = 0L,
        dueEpochDay = due,
        priority = priority,
        completedEpochDay = completed
    )

    @Test
    fun isOverdue_onlyForOpenTasksBeforeToday() {
        assertTrue(TaskRules.isOverdue(task(due = today - 1), today))
        assertFalse(TaskRules.isOverdue(task(due = today), today))
        assertFalse(TaskRules.isOverdue(task(due = today - 1, isDone = true), today))
        assertFalse(TaskRules.isOverdue(task(due = null), today))
    }

    @Test
    fun todayTasks_hidesFutureAndOldDoneTasks() {
        val all = listOf(
            task(id = 1, due = today + 1),
            task(id = 2, due = today),
            task(id = 3, due = null),
            task(id = 4, isDone = true, completed = today - 1),
            task(id = 5, isDone = true, completed = today)
        )
        val ids = TaskRules.todayTasks(all, today).map { it.id }.toSet()
        assertEquals(setOf(2L, 3L, 5L), ids)
    }

    @Test
    fun todayTasks_overdueFirstAndDoneLast() {
        val all = listOf(
            task(id = 1, isDone = true, completed = today),
            task(id = 2, due = today),
            task(id = 3, due = today - 2)
        )
        val ids = TaskRules.todayTasks(all, today).map { it.id }
        assertEquals(listOf(3L, 2L, 1L), ids)
    }

    @Test
    fun reason_overdueAndHighPriority() {
        assertEquals("Overdue", TaskRules.reason(task(due = today - 1), today))
        assertEquals("High priority", TaskRules.reason(task(priority = 1), today))
        assertNull(TaskRules.reason(task(isDone = true), today))
    }

    @Test
    fun dayLabel_names() {
        assertEquals("Today", TaskRules.dayLabel(today, today))
        assertEquals("Tomorrow", TaskRules.dayLabel(today + 1, today))
        assertEquals("Yesterday", TaskRules.dayLabel(today - 1, today))
    }
}

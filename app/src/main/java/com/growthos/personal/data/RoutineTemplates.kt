package com.growthos.personal.data

import android.content.Context
import com.growthos.personal.reminders.ReminderScheduler

class RoutineTemplate(val name: String, val items: List<Pair<Int, String>>)

/** Block name used for the headers in the routine timeline. */
fun routineBlock(minute: Int): String = when {
    minute < 12 * 60 -> "Morning"
    minute < 17 * 60 -> "Afternoon"
    minute < 21 * 60 -> "Evening"
    else -> "Bedtime"
}

object RoutineTemplates {

    val MY_DAY: List<Pair<Int, String>> = listOf(
        315 to "Get ready and go running",
        420 to "Brush teeth and bring milk",
        450 to "Coding and breakfast (finish before 10:00 AM)",
        600 to "Computer class",
        720 to "Return home, self-improvement time",
        780 to "Lunch and milk",
        810 to "Coding session",
        960 to "Outdoor time or walk",
        1140 to "Coding session",
        1305 to "Dinner (by 10:00 PM)",
        1320 to "Wind-down and prepare for sleep",
        1380 to "Target bedtime"
    )

    private val LIGHT_DAY: List<Pair<Int, String>> = listOf(
        360 to "Wake up and stretch",
        450 to "Breakfast",
        540 to "Main focus task (one session)",
        780 to "Lunch and rest",
        960 to "Short walk outside",
        1080 to "One light study or coding session",
        1260 to "Dinner",
        1320 to "Wind-down and prepare for sleep",
        1380 to "Target bedtime"
    )

    private val WORKOUT_DAY: List<Pair<Int, String>> = listOf(
        300 to "Wake up and warm-up",
        315 to "Workout",
        405 to "Shower and breakfast",
        450 to "Coding session",
        600 to "Computer class",
        780 to "Lunch",
        810 to "Coding session",
        960 to "Cool-down walk and stretching",
        1140 to "Study session",
        1305 to "Dinner (by 10:00 PM)",
        1320 to "Wind-down and prepare for sleep",
        1380 to "Target bedtime"
    )

    val all: List<RoutineTemplate> = listOf(
        RoutineTemplate("My day", MY_DAY),
        RoutineTemplate("Light day", LIGHT_DAY),
        RoutineTemplate("Workout day", WORKOUT_DAY)
    )

    /** Adds the template items that are not already in the routine. Returns how many were added. */
    suspend fun add(context: Context, template: RoutineTemplate): Int {
        val dao = AppDatabase.get(context).routineDao()
        val existing = dao.getAll()
        val fresh = template.items.filter { pair ->
            existing.none { it.startMinute == pair.first && it.title == pair.second }
        }
        dao.insertAll(fresh.map { RoutineItemEntity(title = it.second, startMinute = it.first) })
        return fresh.size
    }

    /** Deletes the whole routine (and its ticks) and puts the template in its place. */
    suspend fun replace(context: Context, template: RoutineTemplate) {
        val dao = AppDatabase.get(context).routineDao()
        for (item in dao.getAll()) {
            ReminderScheduler.cancelRoutine(context, item.id)
        }
        dao.deleteAllChecks()
        dao.deleteAllItems()
        dao.insertAll(template.items.map { RoutineItemEntity(title = it.second, startMinute = it.first) })
    }
}

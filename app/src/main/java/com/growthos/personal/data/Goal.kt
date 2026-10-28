package com.growthos.personal.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val horizon: String,
    val progress: Int,
    val createdEpochDay: Long,
    val isArchived: Boolean = false,
    val deadlineEpochDay: Long? = null,
    val completedEpochDay: Long? = null
)

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE isArchived = 0 ORDER BY id ASC")
    fun observeActive(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun get(id: Long): GoalEntity?

    @Insert
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Query("UPDATE goals SET progress = :progress WHERE id = :id")
    suspend fun setProgress(id: Long, progress: Int)

    @Query("UPDATE goals SET progress = :progress, completedEpochDay = :completed WHERE id = :id")
    suspend fun setProgressAndCompleted(id: Long, progress: Int, completed: Long?)

    @Query("UPDATE goals SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("UPDATE goals SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: Long)
}

@Entity(tableName = "goal_milestones")
data class GoalMilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val title: String,
    val isDone: Boolean = false
)

@Dao
interface GoalMilestoneDao {
    @Query("SELECT * FROM goal_milestones ORDER BY id ASC")
    fun observeAll(): Flow<List<GoalMilestoneEntity>>

    @Query("SELECT * FROM goal_milestones WHERE goalId = :goalId ORDER BY id ASC")
    suspend fun getForGoal(goalId: Long): List<GoalMilestoneEntity>

    @Insert
    suspend fun insert(item: GoalMilestoneEntity): Long

    @Query("UPDATE goal_milestones SET isDone = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("UPDATE goal_milestones SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("DELETE FROM goal_milestones WHERE id = :id")
    suspend fun delete(id: Long)
}

object GoalRules {

    /** Sets the progress and keeps completedEpochDay in step (100% = completed today). */
    suspend fun setProgress(context: Context, goal: GoalEntity, progress: Int) {
        val p = progress.coerceIn(0, 100)
        val completed = if (p >= 100) (goal.completedEpochDay ?: LocalDate.now().toEpochDay()) else null
        AppDatabase.get(context).goalDao().setProgressAndCompleted(goal.id, p, completed)
    }

    // With milestones the progress is the share of milestones that are done.
    private suspend fun sync(context: Context, goalId: Long) {
        val db = AppDatabase.get(context)
        val goal = db.goalDao().get(goalId) ?: return
        val list = db.goalMilestoneDao().getForGoal(goalId)
        if (list.isEmpty()) return
        setProgress(context, goal, list.count { it.isDone } * 100 / list.size)
    }

    suspend fun addMilestone(context: Context, goalId: Long, title: String) {
        AppDatabase.get(context).goalMilestoneDao().insert(GoalMilestoneEntity(goalId = goalId, title = title))
        sync(context, goalId)
    }

    suspend fun toggleMilestone(context: Context, item: GoalMilestoneEntity) {
        AppDatabase.get(context).goalMilestoneDao().setDone(item.id, !item.isDone)
        sync(context, item.goalId)
    }

    suspend fun deleteMilestone(context: Context, item: GoalMilestoneEntity) {
        AppDatabase.get(context).goalMilestoneDao().delete(item.id)
        sync(context, item.goalId)
    }

    /** Advice when progress is 15 points or more below the straight-line plan to the deadline. */
    fun advice(goal: GoalEntity, milestones: List<GoalMilestoneEntity>, today: Long): String? {
        val deadline = goal.deadlineEpochDay ?: return null
        if (goal.completedEpochDay != null || goal.progress >= 100) return null
        if (deadline < today) {
            return "Deadline passed. Move the deadline or split this goal into smaller milestones."
        }
        val span = deadline - goal.createdEpochDay
        if (span <= 0) return null
        val planned = ((today - goal.createdEpochDay) * 100 / span).toInt().coerceIn(0, 100)
        if (planned - goal.progress < 15) return null
        val daysLeft = (deadline - today).toInt()
        val left = milestones.count { !it.isDone }
        val base = "Behind schedule: the plan says about $planned% by today, you are at ${goal.progress}%."
        return if (left > 0) {
            if (daysLeft <= 0) {
                "$base Finish $left milestones today."
            } else {
                val every = maxOf(1, daysLeft / left)
                "$base Finish $left milestones in $daysLeft days (about one every $every days)."
            }
        } else {
            "$base ${100 - goal.progress}% is left. Add milestones to split the work."
        }
    }
}

package com.growthos.personal.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// ---------------------------------------------------------------------------
// Learn: courses, topics, skills
// ---------------------------------------------------------------------------

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val deadlineEpochDay: Long? = null,
    val isArchived: Boolean = false
)

// status: 0 not started, 1 learning, 2 done
@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val name: String,
    val status: Int = 0,
    val weak: Boolean = false
)

// status: 0 to learn, 1 learning, 2 done
@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val status: Int = 0,
    val position: Int = 0
)

@Dao
interface LearnDao {
    @Query("SELECT * FROM courses WHERE isArchived = 0 ORDER BY id ASC")
    fun observeCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM topics ORDER BY id ASC")
    fun observeTopics(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM skills ORDER BY position ASC, id ASC")
    fun observeSkills(): Flow<List<SkillEntity>>

    @Insert
    suspend fun insertCourse(course: CourseEntity): Long

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Query("UPDATE courses SET isArchived = 1 WHERE id = :id")
    suspend fun archiveCourse(id: Long)

    @Query("DELETE FROM topics WHERE courseId = :courseId")
    suspend fun deleteTopicsOf(courseId: Long)

    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun deleteCourse(id: Long)

    @Insert
    suspend fun insertTopic(topic: TopicEntity): Long

    @Query("UPDATE topics SET status = :status WHERE id = :id")
    suspend fun setTopicStatus(id: Long, status: Int)

    @Query("UPDATE topics SET weak = :weak WHERE id = :id")
    suspend fun setTopicWeak(id: Long, weak: Boolean)

    @Query("UPDATE topics SET name = :name WHERE id = :id")
    suspend fun renameTopic(id: Long, name: String)

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun deleteTopic(id: Long)

    @Insert
    suspend fun insertSkill(skill: SkillEntity): Long

    @Query("SELECT COALESCE(MAX(position), 0) FROM skills")
    suspend fun maxSkillPosition(): Int

    @Query("UPDATE skills SET status = :status WHERE id = :id")
    suspend fun setSkillStatus(id: Long, status: Int)

    @Query("UPDATE skills SET name = :name WHERE id = :id")
    suspend fun renameSkill(id: Long, name: String)

    @Query("UPDATE skills SET position = :position WHERE id = :id")
    suspend fun setSkillPosition(id: Long, position: Int)

    @Query("DELETE FROM skills WHERE id = :id")
    suspend fun deleteSkill(id: Long)
}

// ---------------------------------------------------------------------------
// Projects: milestones (parentId 0) and subtasks (parentId = milestone id)
// ---------------------------------------------------------------------------

@Entity(tableName = "project_items")
data class ProjectItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    @ColumnInfo(defaultValue = "0") val parentId: Long = 0,
    val title: String,
    val isDone: Boolean = false
)

@Dao
interface ProjectItemDao {
    @Query("SELECT * FROM project_items ORDER BY id ASC")
    fun observeAll(): Flow<List<ProjectItemEntity>>

    @Query("SELECT * FROM project_items WHERE projectId = :projectId ORDER BY id ASC")
    suspend fun getForProject(projectId: Long): List<ProjectItemEntity>

    @Insert
    suspend fun insert(item: ProjectItemEntity): Long

    @Query("UPDATE project_items SET isDone = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("UPDATE project_items SET isDone = :done WHERE parentId = :parentId")
    suspend fun setChildrenDone(parentId: Long, done: Boolean)

    @Query("UPDATE project_items SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("DELETE FROM project_items WHERE id = :id OR parentId = :id")
    suspend fun deleteWithChildren(id: Long)
}

/** Keeps the project progress in step with its milestones. */
object ProjectRules {

    private suspend fun sync(context: Context, projectId: Long) {
        val db = AppDatabase.get(context)
        val milestones = db.projectItemDao().getForProject(projectId).filter { it.parentId == 0L }
        if (milestones.isEmpty()) return
        db.projectDao().setProgress(projectId, milestones.count { it.isDone } * 100 / milestones.size)
    }

    suspend fun addMilestone(context: Context, projectId: Long, title: String) {
        AppDatabase.get(context).projectItemDao().insert(ProjectItemEntity(projectId = projectId, title = title))
        sync(context, projectId)
    }

    suspend fun addSubtask(context: Context, milestone: ProjectItemEntity, title: String) {
        val dao = AppDatabase.get(context).projectItemDao()
        dao.insert(ProjectItemEntity(projectId = milestone.projectId, parentId = milestone.id, title = title))
        dao.setDone(milestone.id, false)
        sync(context, milestone.projectId)
    }

    suspend fun toggleMilestone(context: Context, item: ProjectItemEntity) {
        val dao = AppDatabase.get(context).projectItemDao()
        val done = !item.isDone
        dao.setDone(item.id, done)
        dao.setChildrenDone(item.id, done)
        sync(context, item.projectId)
    }

    suspend fun toggleSubtask(context: Context, item: ProjectItemEntity) {
        val dao = AppDatabase.get(context).projectItemDao()
        dao.setDone(item.id, !item.isDone)
        refreshParent(context, item)
        sync(context, item.projectId)
    }

    suspend fun delete(context: Context, item: ProjectItemEntity) {
        val dao = AppDatabase.get(context).projectItemDao()
        dao.deleteWithChildren(item.id)
        if (item.parentId != 0L) refreshParent(context, item)
        sync(context, item.projectId)
    }

    // A milestone is done when it has subtasks and all of them are done.
    private suspend fun refreshParent(context: Context, child: ProjectItemEntity) {
        val dao = AppDatabase.get(context).projectItemDao()
        val siblings = dao.getForProject(child.projectId).filter { it.parentId == child.parentId }
        if (siblings.isNotEmpty()) dao.setDone(child.parentId, siblings.all { it.isDone })
    }
}

// ---------------------------------------------------------------------------
// Fitness: exercise sets, body measurements, weekly plan
// ---------------------------------------------------------------------------

@Entity(tableName = "exercise_logs")
data class ExerciseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val exercise: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Double
)

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise_logs WHERE epochDay >= :fromDay ORDER BY epochDay DESC, id DESC")
    fun observeSince(fromDay: Long): Flow<List<ExerciseLogEntity>>

    @Insert
    suspend fun insert(log: ExerciseLogEntity): Long

    @Query("DELETE FROM exercise_logs WHERE id = :id")
    suspend fun delete(id: Long)
}

@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val kind: String,
    val value: Double
)

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements ORDER BY epochDay DESC, id DESC")
    fun observeAll(): Flow<List<MeasurementEntity>>

    @Insert
    suspend fun insert(m: MeasurementEntity): Long

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun delete(id: Long)
}

// dayOfWeek: 1 = Monday ... 7 = Sunday
@Entity(tableName = "workout_plan")
data class WorkoutPlanEntity(
    @PrimaryKey val dayOfWeek: Int,
    val name: String
)

@Dao
interface WorkoutPlanDao {
    @Query("SELECT * FROM workout_plan")
    fun observeAll(): Flow<List<WorkoutPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(day: WorkoutPlanEntity)

    @Query("DELETE FROM workout_plan WHERE dayOfWeek = :dayOfWeek")
    suspend fun delete(dayOfWeek: Int)
}

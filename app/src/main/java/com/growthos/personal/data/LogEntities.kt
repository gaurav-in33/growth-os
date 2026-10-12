package com.growthos.personal.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "study_logs")
data class StudyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val kind: String,
    val minutes: Int,
    val topic: String
)

@Dao
interface StudyDao {
    @Query("SELECT * FROM study_logs WHERE epochDay >= :fromDay ORDER BY id DESC")
    fun observeSince(fromDay: Long): Flow<List<StudyLogEntity>>

    @Insert
    suspend fun insert(log: StudyLogEntity): Long

    @Query("DELETE FROM study_logs WHERE id = :id")
    suspend fun delete(id: Long)
}

@Entity(tableName = "workout_logs")
data class WorkoutLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val type: String,
    val minutes: Int
)

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_logs WHERE epochDay >= :fromDay ORDER BY id DESC")
    fun observeSince(fromDay: Long): Flow<List<WorkoutLogEntity>>

    @Insert
    suspend fun insert(log: WorkoutLogEntity): Long

    @Query("DELETE FROM workout_logs WHERE id = :id")
    suspend fun delete(id: Long)
}

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val progress: Int,
    val createdEpochDay: Long,
    val isArchived: Boolean = false
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE isArchived = 0 ORDER BY id ASC")
    fun observeActive(): Flow<List<ProjectEntity>>

    @Insert
    suspend fun insert(project: ProjectEntity): Long

    @Query("UPDATE projects SET progress = :progress WHERE id = :id")
    suspend fun setProgress(id: Long, progress: Int)

    @Query("UPDATE projects SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE projects SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: Long)
}

@Entity(tableName = "day_checkins")
data class DayCheckin(
    @PrimaryKey val epochDay: Long,
    val mood: Int,
    val energy: Int,
    val sleepMinutes: Int
)

@Dao
interface CheckinDao {
    @Query("SELECT * FROM day_checkins WHERE epochDay = :epochDay")
    fun observe(epochDay: Long): Flow<DayCheckin?>

    @Query("SELECT * FROM day_checkins WHERE epochDay >= :fromDay AND epochDay <= :toDay")
    fun observeBetween(fromDay: Long, toDay: Long): Flow<List<DayCheckin>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(checkin: DayCheckin)
}

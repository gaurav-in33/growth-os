package com.growthos.personal.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val horizon: String,
    val progress: Int,
    val createdEpochDay: Long,
    val isArchived: Boolean = false
)

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE isArchived = 0 ORDER BY id ASC")
    fun observeActive(): Flow<List<GoalEntity>>

    @Insert
    suspend fun insert(goal: GoalEntity): Long

    @Query("UPDATE goals SET progress = :progress WHERE id = :id")
    suspend fun setProgress(id: Long, progress: Int)

    @Query("UPDATE goals SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("UPDATE goals SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: Long)
}

package com.growthos.personal.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val endedAtMillis: Long,
    val minutes: Int
)

@Dao
interface FocusDao {
    @Query("SELECT COALESCE(SUM(minutes), 0) FROM focus_sessions WHERE endedAtMillis >= :sinceMillis")
    fun observeMinutesSince(sinceMillis: Long): Flow<Int>

    @Insert
    suspend fun insert(session: FocusSessionEntity): Long
}

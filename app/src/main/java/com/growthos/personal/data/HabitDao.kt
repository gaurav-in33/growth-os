package com.growthos.personal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY id ASC")
    fun observeActive(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_checkins")
    fun observeCheckins(): Flow<List<HabitCheckin>>

    @Insert
    suspend fun insertHabit(habit: HabitEntity): Long

    @Query("UPDATE habits SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCheckin(checkin: HabitCheckin)

    @Query("DELETE FROM habit_checkins WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteCheckin(habitId: Long, epochDay: Long)
}

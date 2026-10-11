package com.growthos.personal.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdEpochDay: Long,
    val isArchived: Boolean = false
)

@Entity(tableName = "habit_checkins", primaryKeys = ["habitId", "epochDay"])
data class HabitCheckin(
    val habitId: Long,
    val epochDay: Long
)

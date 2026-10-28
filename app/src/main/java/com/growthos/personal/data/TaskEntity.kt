package com.growthos.personal.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val isDone: Boolean = false,
    val createdAtMillis: Long,
    val dueEpochDay: Long? = null,
    val dueMinute: Int? = null,
    // 1 high, 2 medium, 3 low
    @ColumnInfo(defaultValue = "2") val priority: Int = 2,
    // 0 = none
    @ColumnInfo(defaultValue = "0") val estimateMin: Int = 0,
    @ColumnInfo(defaultValue = "''") val category: String = "",
    // 0 none, 1 low, 2 med, 3 high
    @ColumnInfo(defaultValue = "0") val energy: Int = 0,
    // none, daily, weekly
    @ColumnInfo(defaultValue = "'none'") val repeat: String = "none",
    @ColumnInfo(defaultValue = "0") val remind: Boolean = false,
    val completedEpochDay: Long? = null
)

package com.growthos.personal.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "wake_confirmations")
data class WakeConfirmation(
    @PrimaryKey val epochDay: Long,
    val confirmedAtMillis: Long
)

@Dao
interface WakeDao {
    @Query("SELECT confirmedAtMillis FROM wake_confirmations WHERE epochDay = :epochDay")
    fun observe(epochDay: Long): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(confirmation: WakeConfirmation)
}

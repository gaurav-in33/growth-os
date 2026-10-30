package com.growthos.personal.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "water_days")
data class WaterDay(
    @PrimaryKey val epochDay: Long,
    val glasses: Int
)

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_days WHERE epochDay >= :fromDay")
    fun observeSince(fromDay: Long): Flow<List<WaterDay>>

    @Query("SELECT glasses FROM water_days WHERE epochDay = :epochDay")
    fun observe(epochDay: Long): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(day: WaterDay)
}

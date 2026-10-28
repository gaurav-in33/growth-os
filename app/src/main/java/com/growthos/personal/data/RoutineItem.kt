package com.growthos.personal.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "routine_items")
data class RoutineItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startMinute: Int,
    val remind: Boolean = false
)

@Entity(tableName = "routine_checks", primaryKeys = ["routineId", "epochDay"])
data class RoutineCheck(
    val routineId: Long,
    val epochDay: Long
)

@Dao
interface RoutineDao {
    @Query("SELECT routineId FROM routine_checks WHERE epochDay = :epochDay")
    fun observeCheckedIds(epochDay: Long): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCheck(check: RoutineCheck)

    @Query("DELETE FROM routine_checks WHERE routineId = :id AND epochDay = :epochDay")
    suspend fun deleteCheck(id: Long, epochDay: Long)

    @Query("DELETE FROM routine_checks WHERE routineId = :id")
    suspend fun deleteChecksFor(id: Long)

    @Query("DELETE FROM routine_checks")
    suspend fun deleteAllChecks()

    @Query("DELETE FROM routine_items")
    suspend fun deleteAllItems()

    @Query("SELECT * FROM routine_items ORDER BY startMinute ASC, id ASC")
    fun observeAll(): Flow<List<RoutineItemEntity>>

    @Query("SELECT * FROM routine_items")
    suspend fun getAll(): List<RoutineItemEntity>

    @Query("SELECT COUNT(*) FROM routine_items")
    suspend fun count(): Int

    @Insert
    suspend fun insert(item: RoutineItemEntity): Long

    @Insert
    suspend fun insertAll(items: List<RoutineItemEntity>)

    @Update
    suspend fun update(item: RoutineItemEntity)

    @Query("UPDATE routine_items SET remind = :remind WHERE id = :id")
    suspend fun setRemind(id: Long, remind: Boolean)

    @Query("DELETE FROM routine_items WHERE id = :id")
    suspend fun delete(id: Long)
}

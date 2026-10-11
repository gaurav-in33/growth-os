package com.growthos.personal.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "finance_entries")
data class FinanceEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val kind: String,
    val amountPaise: Long,
    val category: String,
    val note: String
)

@Dao
interface FinanceDao {
    @Query("SELECT * FROM finance_entries WHERE epochDay >= :fromDay AND epochDay <= :toDay ORDER BY epochDay DESC, id DESC")
    fun observeBetween(fromDay: Long, toDay: Long): Flow<List<FinanceEntryEntity>>

    @Insert
    suspend fun insert(entry: FinanceEntryEntity): Long

    @Query("DELETE FROM finance_entries WHERE id = :id")
    suspend fun delete(id: Long)
}

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAtMillis: Long,
    val title: String,
    val body: String
)

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY id DESC LIMIT 50")
    fun observeLatest(): Flow<List<NoteEntity>>

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)
}

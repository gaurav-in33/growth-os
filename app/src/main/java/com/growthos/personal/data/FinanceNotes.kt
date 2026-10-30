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
    @Query("UPDATE finance_entries SET epochDay = :epochDay, kind = :kind, amountPaise = :amountPaise, category = :category, note = :note WHERE id = :id")
    suspend fun updateFull(id: Long, epochDay: Long, kind: String, amountPaise: Long, category: String, note: String)

    @Query("SELECT * FROM finance_entries WHERE epochDay >= :fromDay AND epochDay <= :toDay ORDER BY epochDay DESC, id DESC")
    fun observeBetween(fromDay: Long, toDay: Long): Flow<List<FinanceEntryEntity>>

    @Insert
    suspend fun insert(entry: FinanceEntryEntity): Long

    @Query("UPDATE finance_entries SET amountPaise = :amountPaise, note = :note WHERE id = :id")
    suspend fun update(id: Long, amountPaise: Long, note: String)

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

    @Query("UPDATE notes SET title = :title, body = :body WHERE id = :id")
    suspend fun update(id: Long, title: String, body: String)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)
}

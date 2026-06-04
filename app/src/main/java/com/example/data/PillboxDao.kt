package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface PillboxDao {
    @Query("SELECT * FROM pillboxes ORDER BY id DESC")
    fun getAllPillboxes(): Flow<List<Pillbox>>

    @Query("SELECT * FROM pillboxes ORDER BY id DESC")
    suspend fun getAllPillboxesDirect(): List<Pillbox>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPillbox(pillbox: Pillbox): Long

    @Update
    suspend fun updatePillbox(pillbox: Pillbox)

    @Delete
    suspend fun deletePillbox(pillbox: Pillbox)

    @Query("SELECT * FROM pillbox_entries ORDER BY id DESC")
    fun getAllPillboxEntries(): Flow<List<PillboxEntry>>

    @Query("SELECT * FROM pillbox_entries ORDER BY id DESC")
    suspend fun getAllPillboxEntriesDirect(): List<PillboxEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPillboxEntry(entry: PillboxEntry): Long

    @Update
    suspend fun updatePillboxEntry(entry: PillboxEntry)

    @Delete
    suspend fun deletePillboxEntry(entry: PillboxEntry)

    @Query("SELECT * FROM pillbox_entries WHERE id = :id LIMIT 1")
    suspend fun getPillboxEntryById(id: Int): PillboxEntry?
}

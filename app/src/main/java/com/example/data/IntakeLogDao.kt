package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IntakeLogDao {
    @Query("SELECT * FROM intake_logs ORDER BY takenTimestamp DESC")
    fun getAllIntakeLogs(): Flow<List<IntakeLog>>

    @Query("SELECT * FROM intake_logs ORDER BY takenTimestamp DESC")
    suspend fun getAllIntakeLogsDirect(): List<IntakeLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntakeLog(log: IntakeLog): Long

    @Delete
    suspend fun deleteIntakeLog(log: IntakeLog)

    @Query("DELETE FROM intake_logs")
    suspend fun deleteAllIntakeLogs()
}

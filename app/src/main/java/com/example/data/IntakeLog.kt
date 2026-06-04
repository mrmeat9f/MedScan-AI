package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "intake_logs")
data class IntakeLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicineName: String,
    val dosage: Double,
    val takenTimestamp: Long = System.currentTimeMillis(),
    val pillboxName: String = "",
    val scheduledTime: String = "" // e.g. "08:00"
) : Serializable

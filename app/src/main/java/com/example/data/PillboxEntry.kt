package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "pillbox_entries")
data class PillboxEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pillboxId: Int = 0,
    val medicineName: String,
    val dosage: Double = 1.0,
    val preferredTime: String = "12:00", // "HH:mm" format
    val periodicityDays: Int = 1, // 1 for every day, 2 for every other day, 3 for every 3 days etc.
    val lastTakenTimestamp: Long = 0L
) : Serializable

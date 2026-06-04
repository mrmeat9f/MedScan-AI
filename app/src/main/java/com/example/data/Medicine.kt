package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val gtin: String,
    val serial: String,
    val expirationDate: String, // format "YYYY-MM-DD" or similar
    val expirationTimestamp: Long, // to easily sort and filter
    val scannedAt: Long = System.currentTimeMillis(),
    val batch: String = "",
    val count: Int = 1,
    val notes: String = "",
    val totalPackageCount: Int = 30,
    val remainingCount: Double = 30.0,
    val intakeDosage: Double = 0.0,
    val intakeFrequency: String = "as_needed",
    val lastIntakeDecayTimestamp: Long = System.currentTimeMillis(),
    val packageImagePath: String? = null,
    val tags: String = "" // Added column for tags, stored as comma-separated string
) : Serializable

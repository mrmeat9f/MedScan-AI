package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "pillboxes")
data class Pillbox(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val notificationsEnabled: Boolean = true
) : Serializable

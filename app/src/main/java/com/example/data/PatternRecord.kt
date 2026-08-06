package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pattern_records")
data class PatternRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val timestamp: Long = System.currentTimeMillis(),
    val summary: String,
    val rawMetrics: String,
    val AIInsights: String = "",
    val imageUri: String? = null
)

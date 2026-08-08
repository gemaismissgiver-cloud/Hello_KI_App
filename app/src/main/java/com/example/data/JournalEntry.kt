package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val type: String = "AUTONOMOUS_JOURNAL", // "SNAPSHOT", "AUTONOMOUS_JOURNAL", "FACT", "PREFERENCE"
    val importanceScore: Int = 5, // 1 to 10
    val createdBy: String = "KI", // "KI", "Nutzer", "System"
    val timestamp: Long = System.currentTimeMillis()
)

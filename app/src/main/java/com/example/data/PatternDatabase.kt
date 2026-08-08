package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PatternRecord::class, JournalEntry::class], version = 2, exportSchema = false)
abstract class PatternDatabase : RoomDatabase() {
    abstract fun patternDao(): PatternDao
    abstract fun journalDao(): JournalDao

    companion object {
        @Volatile
        private var INSTANCE: PatternDatabase? = null

        fun getDatabase(context: Context): PatternDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PatternDatabase::class.java,
                    "pattern_studio_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

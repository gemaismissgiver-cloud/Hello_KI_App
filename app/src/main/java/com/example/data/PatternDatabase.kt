package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PatternRecord::class], version = 1, exportSchema = false)
abstract class PatternDatabase : RoomDatabase() {
    abstract fun patternDao(): PatternDao

    companion object {
        @Volatile
        private var INSTANCE: PatternDatabase? = null

        fun getDatabase(context: Context): PatternDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PatternDatabase::class.java,
                    "pattern_studio_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

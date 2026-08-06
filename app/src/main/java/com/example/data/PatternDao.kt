package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PatternDao {
    @Query("SELECT * FROM pattern_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<PatternRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: PatternRecord): Long

    @Query("DELETE FROM pattern_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM pattern_records")
    suspend fun clearAll()
}

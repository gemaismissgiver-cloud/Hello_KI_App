package com.example.data

import kotlinx.coroutines.flow.Flow

class PatternRepository(private val patternDao: PatternDao) {
    val allRecords: Flow<List<PatternRecord>> = patternDao.getAllRecords()

    suspend fun insert(record: PatternRecord): Long {
        return patternDao.insertRecord(record)
    }

    suspend fun deleteById(id: Long) {
        patternDao.deleteRecordById(id)
    }

    suspend fun clearAll() {
        patternDao.clearAll()
    }
}

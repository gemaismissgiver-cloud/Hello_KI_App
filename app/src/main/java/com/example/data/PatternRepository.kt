package com.example.data

import kotlinx.coroutines.flow.Flow

class PatternRepository(
    private val patternDao: PatternDao,
    private val journalDao: JournalDao
) {
    val allRecords: Flow<List<PatternRecord>> = patternDao.getAllRecords()
    val allJournalEntries: Flow<List<JournalEntry>> = journalDao.getAllEntries()

    suspend fun insert(record: PatternRecord): Long {
        return patternDao.insertRecord(record)
    }

    suspend fun deleteById(id: Long) {
        patternDao.deleteRecordById(id)
    }

    suspend fun clearAll() {
        patternDao.clearAll()
    }

    suspend fun insertJournalEntry(entry: JournalEntry): Long {
        return journalDao.insertEntry(entry)
    }

    suspend fun deleteJournalEntryById(id: Long) {
        journalDao.deleteEntryById(id)
    }

    suspend fun clearJournal() {
        journalDao.clearAll()
    }

    suspend fun getJournalEntriesList(): List<JournalEntry> {
        return journalDao.getAllEntriesList()
    }
}

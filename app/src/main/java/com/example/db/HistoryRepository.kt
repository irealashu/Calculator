package com.example.db

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<HistoryItem>> = historyDao.getAllHistory()

    suspend fun insert(item: HistoryItem) = historyDao.insertHistory(item)

    suspend fun delete(item: HistoryItem) = historyDao.deleteHistory(item)

    suspend fun clearAll() = historyDao.clearAllHistory()
}

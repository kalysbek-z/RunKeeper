package com.example.runkeeper.repositories

import com.example.runkeeper.database.Run
import com.example.runkeeper.database.RunDao
import javax.inject.Inject

class HistoryRepository @Inject constructor(
    val runDao: RunDao
) {
    suspend fun insertRun(run: Run) = runDao.insertRun(run)

    suspend fun deleteRun(run: Run) = runDao.deleteRun(run)

    fun getAllRuns() = runDao.getAllRuns()
}
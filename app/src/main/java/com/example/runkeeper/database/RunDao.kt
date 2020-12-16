package com.example.runkeeper.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface RunDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: Run)

    @Delete
    suspend fun deleteRun(run: Run)

    @Query("SELECT * FROM history_table ORDER BY timestamp DESC")
    fun getAllRuns(): LiveData<List<Run>>
}
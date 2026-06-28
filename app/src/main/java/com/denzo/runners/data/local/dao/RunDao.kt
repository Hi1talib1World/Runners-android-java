package com.denzo.runners.data.local.dao

import androidx.room.*
import com.denzo.runners.data.local.entities.RunEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: RunEntity)

    @Delete
    suspend fun deleteRun(run: RunEntity)

    @Query("SELECT * FROM runs ORDER BY timestamp DESC")
    fun getAllRuns(): Flow<List<RunEntity>>

    @Query("SELECT * FROM runs WHERE id = :id")
    suspend fun getRunById(id: Int): RunEntity?

    @Query("SELECT * FROM runs WHERE isSynced = 0")
    suspend fun getUnsyncedRuns(): List<RunEntity>

    @Query("UPDATE runs SET isSynced = 1 WHERE id = :runId")
    suspend fun markAsSynced(runId: Int)

    @Query("DELETE FROM runs")
    suspend fun deleteAll()
}

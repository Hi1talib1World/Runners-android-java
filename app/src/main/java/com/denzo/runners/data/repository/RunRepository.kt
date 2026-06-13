package com.denzo.runners.data.repository

import com.denzo.runners.data.local.dao.RunDao
import com.denzo.runners.data.local.entities.RunEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunRepository @Inject constructor(
    private val runDao: RunDao
) {
    fun getAllRuns(): Flow<List<RunEntity>> = runDao.getAllRuns()

    suspend fun saveRun(run: RunEntity) = runDao.insertRun(run)

    suspend fun clearAllHistory() = runDao.deleteAll()
}

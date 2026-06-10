package com.denzo.runners.data.remote.repository

import com.denzo.runners.data.local.dao.RunningDAO
import com.denzo.runners.data.local.entities.Runningdata
import com.denzo.runners.data.remote.api.RunnersApiService
import com.denzo.runners.data.remote.dto.TelemetryBatchDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepository @Inject constructor(
    private val apiService: RunnersApiService,
    private val localDao: RunningDAO
) {
    suspend fun getActivities() = localDao.getAllRuningdata()

    suspend fun saveActivity(activity: Runningdata) {
        localDao.insert(activity)
        // Sync with cloud
        try {
            apiService.syncActivity(activity)
        } catch (e: Exception) {
            // Handle sync failure (e.g., mark as pending sync)
        }
    }

    suspend fun uploadTelemetry(batch: TelemetryBatchDto) {
        try {
            apiService.uploadTelemetryBatch(batch)
        } catch (e: Exception) {
            // Robust ingestion: failure should be handled (e.g., retry or local buffer)
        }
    }
}

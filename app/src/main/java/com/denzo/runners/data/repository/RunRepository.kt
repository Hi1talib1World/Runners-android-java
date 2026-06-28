package com.denzo.runners.data.repository

import com.denzo.runners.data.local.dao.GearDao
import com.denzo.runners.data.local.dao.RouteDao
import com.denzo.runners.data.local.dao.RunDao
import com.denzo.runners.data.local.entities.GearEntity
import com.denzo.runners.data.local.entities.RouteEntity
import com.denzo.runners.data.local.entities.RunEntity
import com.denzo.runners.data.remote.api.RunnersApiService
import com.denzo.runners.data.remote.dto.TelemetryBatchDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunRepository @Inject constructor(
    private val runDao: RunDao,
    private val routeDao: RouteDao,
    private val gearDao: GearDao,
    private val apiService: RunnersApiService
) {
    fun getAllRuns(): Flow<List<RunEntity>> = runDao.getAllRuns()

    suspend fun getRunById(id: Int): RunEntity? = runDao.getRunById(id)

    suspend fun saveRun(run: RunEntity) {
        runDao.insertRun(run)
        
        // Update active gear mileage
        val activeGear = gearDao.getActiveGear()
        activeGear?.let {
            val updatedGear = it.copy(currentMileageMeters = it.currentMileageMeters + run.distanceMeters)
            gearDao.updateGear(updatedGear)
        }

        try {
            apiService.uploadRun(run)
            runDao.markAsSynced(run.id)
        } catch (e: Exception) {}
    }

    suspend fun deleteRun(run: RunEntity) = runDao.deleteRun(run)

    suspend fun clearAllHistory() = runDao.deleteAll()

    suspend fun uploadTelemetry(batch: TelemetryBatchDto) {
        try {
            apiService.uploadTelemetryBatch(batch)
        } catch (e: Exception) {}
    }

    // Route Management
    fun getAllRoutes(): Flow<List<RouteEntity>> = routeDao.getAllRoutes()
    suspend fun saveRoute(route: RouteEntity) = routeDao.insertRoute(route)
    suspend fun deleteRoute(route: RouteEntity) = routeDao.deleteRoute(route)
    suspend fun getRouteById(id: Int) = routeDao.getRouteById(id)

    // Gear Management
    fun getAllGear(): Flow<List<GearEntity>> = gearDao.getAllGearFlow()
    suspend fun getActiveGear(): GearEntity? = gearDao.getActiveGear()
    suspend fun addGear(gear: GearEntity) {
        if (gear.isActive) {
            gearDao.deactivateAllGear()
        }
        gearDao.insertGear(gear)
    }
    suspend fun updateGear(gear: GearEntity) = gearDao.updateGear(gear)
    suspend fun setActiveGear(gearId: Int) {
        gearDao.deactivateAllGear()
        gearDao.activateGear(gearId)
    }
    suspend fun deleteGear(gear: GearEntity) = gearDao.deleteGear(gear)
}

package com.denzo.runners.data.local.dao

import androidx.room.*
import com.denzo.runners.data.local.entities.RouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteEntity)

    @Delete
    suspend fun deleteRoute(route: RouteEntity)

    @Query("SELECT * FROM saved_routes ORDER BY timestamp DESC")
    fun getAllRoutes(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM saved_routes WHERE id = :id")
    suspend fun getRouteById(id: Int): RouteEntity?
}

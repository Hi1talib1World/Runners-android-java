package com.denzo.runners.data.remote.api

import com.denzo.runners.data.local.entities.Runningdata
import com.denzo.runners.data.remote.dto.TelemetryBatchDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface RunnersApiService {

    @GET("activities")
    suspend fun getActivities(): List<Runningdata> // Using entity for now, should use DTO

    @POST("activities")
    suspend fun syncActivity(@Body activity: Runningdata)

    @POST("telemetry/batch")
    suspend fun uploadTelemetryBatch(@Body batch: TelemetryBatchDto)
}

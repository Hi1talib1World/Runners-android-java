package com.denzo.runners.data.remote.api

import com.denzo.runners.data.remote.dto.TelemetryBatchDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface RunnersApiService {

    @GET("activities")
    suspend fun getActivities(): List<Any> // Placeholder until DTO is defined

    @POST("activities")
    suspend fun uploadRun(@Body run: com.denzo.runners.data.local.entities.RunEntity)

    @POST("telemetry/batch")
    suspend fun uploadTelemetryBatch(@Body batch: TelemetryBatchDto)
}

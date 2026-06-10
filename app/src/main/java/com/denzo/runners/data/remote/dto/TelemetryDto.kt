package com.denzo.runners.data.remote.dto

data class TelemetryPointDto(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speed: Float,
    val timestamp: Long
)

data class TelemetryBatchDto(
    val userId: String,
    val activityId: String,
    val points: List<TelemetryPointDto>
)

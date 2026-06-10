package com.denzo.runners.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import com.denzo.runners.data.remote.dto.TelemetryBatchDto
import com.denzo.runners.data.remote.dto.TelemetryPointDto
import com.denzo.runners.data.remote.repository.ActivityRepository
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {

    @Inject
    lateinit var repository: ActivityRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var telemetryBuffer = mutableListOf<TelemetryPointDto>()
    private var currentActivityId: String = "session_${System.currentTimeMillis()}"

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { location ->
                addPointToBuffer(location)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationUpdates()
        startBatchSyncTimer()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000) // 1s frequency
            .setMinUpdateIntervalMillis(500)
            .build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, null)
    }

    private fun addPointToBuffer(location: Location) {
        val point = TelemetryPointDto(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            speed = location.speed,
            timestamp = location.time
        )
        synchronized(telemetryBuffer) {
            telemetryBuffer.add(point)
        }
    }

    private fun startBatchSyncTimer() {
        serviceScope.launch {
            while (isActive) {
                delay(TimeUnit.SECONDS.toMillis(15)) // Batch every 15s for ingestion optimization
                syncBatch()
            }
        }
    }

    private suspend fun syncBatch() {
        val pointsToSend = synchronized(telemetryBuffer) {
            val copy = telemetryBuffer.toList()
            telemetryBuffer.clear()
            copy
        }

        if (pointsToSend.isNotEmpty()) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
            val batch = TelemetryBatchDto(
                userId = userId,
                activityId = currentActivityId,
                points = pointsToSend
            )
            repository.uploadTelemetry(batch)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

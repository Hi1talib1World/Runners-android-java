package com.denzo.runners.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.denzo.runners.R
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

    private val CHANNEL_ID = "run_tracking_channel"
    private val NOTIFICATION_ID = 1

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
        
        startForeground(NOTIFICATION_ID, createNotification(), 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            }
        )
        
        startLocationUpdates()
        startBatchSyncTimer()
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Run Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Runners Activity Active")
            .setContentText("Tracking your high-frequency telemetry...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(500)
            .build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
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
                delay(TimeUnit.SECONDS.toMillis(15))
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

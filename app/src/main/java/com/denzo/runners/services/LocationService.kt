package com.denzo.runners.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.denzo.runners.R
import com.denzo.runners.core.utils.AudioCoach
import com.denzo.runners.data.remote.dto.TelemetryBatchDto
import com.denzo.runners.data.remote.dto.TelemetryPointDto
import com.denzo.runners.data.repository.AuthRepository
import com.denzo.runners.data.repository.RunRepository
import com.denzo.runners.features.settings.SettingsRepository
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.osmdroid.util.GeoPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {

    companion object {
        private const val SYNC_INTERVAL_SECONDS = 15L
        private const val TRACKING_INTERVAL_MS = 1000L
        private const val MIN_TRACKING_INTERVAL_MS = 500L
        private const val BATTERY_SAVER_THRESHOLD = 15
    }

    @Inject
    lateinit var repository: RunRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var telemetryBuffer = mutableListOf<TelemetryPointDto>()
    private var currentActivityId: String = "session_${System.currentTimeMillis()}"
    private var runDurationSeconds: Long = 0
    private var lastAnnouncedKm: Int = 0
    private var durationJob: Job? = null
    private var hrJob: Job? = null
    private var audioCoach: AudioCoach? = null
    private var isSocialAlertsEnabled: Boolean = true

    private val CHANNEL_ID = "run_tracking_channel"
    private val NOTIFICATION_ID = 1

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { location ->
                addPointToBuffer(location)
                TrackingManager.updateLocation(
                    GeoPoint(location.latitude, location.longitude),
                    location.speed
                )
                checkAudioSplits()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val routeId = intent?.getIntExtra("routeId", -1) ?: -1
        serviceScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            isSocialAlertsEnabled = settings.isSocialNotificationsEnabled
            val route = if (routeId != -1) repository.getRouteById(routeId) else null
            TrackingManager.startRun(route, settings.maxHeartRate)
        }
        
        // Critical: Guard for GPS availability
        if (!isGpsEnabled()) {
            Toast.makeText(this, "GPS is required for tracking", Toast.LENGTH_LONG).show()
            stopSelf()
        }
        
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        audioCoach = AudioCoach(this)
        
        startForeground(NOTIFICATION_ID, createNotification(), 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            }
        )
        
        startLocationUpdates()
        startBatchSyncTimer()
        startDurationTimer()
        startHrSimulation()
        observeCheers()
    }

    private fun isGpsEnabled(): Boolean {
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun getBatteryLevel(): Int {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else -1
    }

    private fun startHrSimulation() {
        hrJob = serviceScope.launch {
            while (isActive) {
                val simulatedHr = (140..185).random()
                TrackingManager.updateHeartRate(simulatedHr)
                delay(2000)
            }
        }
    }

    private fun observeCheers() {
        serviceScope.launch {
            TrackingManager.cheerEvent.collect { from ->
                if (isSocialAlertsEnabled) {
                    audioCoach?.announceCheer(from)
                }
            }
        }
    }

    private fun checkAudioSplits() {
        val currentKm = (TrackingManager.liveRunState.value.distanceMeters / 1000).toInt()
        if (currentKm > lastAnnouncedKm) {
            lastAnnouncedKm = currentKm
            audioCoach?.announceSplit(currentKm)
        }
    }

    private fun startDurationTimer() {
        durationJob = serviceScope.launch {
            while (isActive) {
                delay(TRACKING_INTERVAL_MS)
                runDurationSeconds++
                TrackingManager.updateDuration(runDurationSeconds)
            }
        }
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

    private fun startLocationUpdates() {
        val battery = getBatteryLevel()
        
        // If-Else Robustness: Adjust tracking frequency based on battery level
        val interval = if (battery != -1 && battery < BATTERY_SAVER_THRESHOLD) {
            TRACKING_INTERVAL_MS * 5 // Slow down tracking to save battery
        } else {
            TRACKING_INTERVAL_MS
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
            .setMinUpdateIntervalMillis(MIN_TRACKING_INTERVAL_MS)
            .build()
        
        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (unlikely: SecurityException) {
            // Permission should be granted by HomeFragment
        }
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
                delay(TimeUnit.SECONDS.toMillis(SYNC_INTERVAL_SECONDS))
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
            val userId = authRepository.userId ?: "anonymous"
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
        audioCoach?.shutdown()
        TrackingManager.stopRun()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        hrJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

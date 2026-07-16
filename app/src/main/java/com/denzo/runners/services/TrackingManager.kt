package com.denzo.runners.services

import com.denzo.runners.BuildConfig
import com.denzo.runners.data.local.entities.RouteEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.osmdroid.util.GeoPoint

data class LiveRunState(
    val isTracking: Boolean = false,
    val isAutoPaused: Boolean = false,
    val pathPoints: List<GeoPoint> = emptyList(),
    val distanceMeters: Double = 0.0,
    val durationSeconds: Long = 0,
    val currentPace: Double = 0.0,
    val activeRoute: RouteEntity? = null,
    val ghostPosition: GeoPoint? = null,
    val timeDeltaSeconds: Long = 0,
    val currentHr: Int = 0,
    val currentHrZone: Int = 0,
    val zoneBreakdown: List<Long> = listOf(0, 0, 0, 0, 0)
)

object TrackingManager {

    private const val MIN_ACCURACY_THRESHOLD = 30.0f // meters
    private const val AUTO_PAUSE_SPEED_THRESHOLD = 0.5f // m/s (approx 1.8 km/h)
    private const val AUTO_PAUSE_GRACE_PERIOD_S = 5 // Don't auto-pause in first 5s

    private val _liveRunState = MutableStateFlow(LiveRunState())
    val liveRunState: StateFlow<LiveRunState> = _liveRunState.asStateFlow()

    private val _cheerEvent = MutableSharedFlow<String>()
    val cheerEvent: SharedFlow<String> = _cheerEvent.asSharedFlow()

    private var maxHr: Int = 190
    private var lastValidLocation: GeoPoint? = null

    fun startRun(route: RouteEntity? = null, userMaxHr: Int = 190) {
        maxHr = userMaxHr
        _liveRunState.update { it.copy(
            isTracking = true, 
            isAutoPaused = false, 
            activeRoute = route,
            durationSeconds = 0,
            distanceMeters = 0.0
        ) }
    }

    fun pauseRun(isAuto: Boolean = false) {
        // If-Else Robustness: Prevent auto-pause during the start grace period
        if (isAuto && _liveRunState.value.durationSeconds < AUTO_PAUSE_GRACE_PERIOD_S) {
            return
        }
        
        // Debug override: Disable auto-pause in debug if desired, or keep it but allow stationary starts
        if (isAuto && BuildConfig.DEBUG) {
            // Log: "Auto-pause suppressed in Debug mode"
            // return 
        }

        _liveRunState.update { it.copy(isTracking = false, isAutoPaused = isAuto) }
    }

    fun resumeRun() {
        _liveRunState.update { it.copy(isTracking = true, isAutoPaused = false) }
    }

    fun stopRun() {
        _liveRunState.update { it.copy(isTracking = false, isAutoPaused = false) }
    }

    fun resetRun() {
        _liveRunState.value = LiveRunState()
        lastValidLocation = null
    }

    fun receiveCheer(from: String) {
        _cheerEvent.tryEmit(from)
    }

    fun updateHeartRate(hr: Int) {
        if (!_liveRunState.value.isTracking) return
        
        val zone = calculateZone(hr, maxHr)
        _liveRunState.update { state ->
            val newBreakdown = state.zoneBreakdown.toMutableList()
            if (zone in 1..5) {
                newBreakdown[zone - 1]++
            }
            state.copy(currentHr = hr, currentHrZone = zone, zoneBreakdown = newBreakdown)
        }
    }

    private fun calculateZone(hr: Int, max: Int): Int {
        val pct = (hr.toDouble() / max) * 100
        return if (pct < 60) 1
        else if (pct < 70) 2
        else if (pct < 80) 3
        else if (pct < 90) 4
        else 5
    }

    fun updateLocation(point: GeoPoint, speed: Float, accuracy: Float = 0f) {
        val currentState = _liveRunState.value
        
        // Logic 1: Filter low accuracy points
        if (accuracy > MIN_ACCURACY_THRESHOLD) return 

        // Logic 2: Auto-Pause/Resume detection
        if (speed < AUTO_PAUSE_SPEED_THRESHOLD) {
            if (currentState.isTracking && !currentState.isAutoPaused) {
                pauseRun(isAuto = true)
            }
            // Even if paused, we might want to update position if it's accurate, 
            // but for tracking distance, we only do it while isTracking is true.
        } else {
            if (currentState.isAutoPaused) {
                resumeRun()
            }
        }

        if (!currentState.isTracking) return

        _liveRunState.update { state ->
            val newPoints = state.pathPoints + point
            var newDistance = state.distanceMeters
            
            // Logic 3: Accurate distance calculation
            lastValidLocation?.let { last ->
                val segment = last.distanceToAsDouble(point)
                // Filter out impossible jumps (GPS artifacts)
                if (segment < 100) { 
                    newDistance += segment
                }
            }
            lastValidLocation = point

            // Logic 4: Ghost Mode positioning
            val ghostPos = state.activeRoute?.let { route ->
                val progressIdx = (state.durationSeconds / 2).toInt()
                if (progressIdx < route.pathPoints.size) {
                    route.pathPoints[progressIdx]
                } else {
                    route.pathPoints.lastOrNull()
                }
            }

            state.copy(
                pathPoints = newPoints,
                distanceMeters = newDistance,
                currentPace = if (speed > 0) (1000.0 / speed / 60.0) else 0.0,
                ghostPosition = ghostPos
            )
        }
    }

    fun updateDuration(totalSeconds: Long) {
        // The timer should always advance if we are not manually paused, 
        // but if auto-paused, we might want to freeze the duration.
        if (_liveRunState.value.isTracking) {
            _liveRunState.update { it.copy(durationSeconds = totalSeconds) }
        }
    }
}

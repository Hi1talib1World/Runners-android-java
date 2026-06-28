package com.denzo.runners.services

import com.denzo.runners.core.utils.GhostManager
import com.denzo.runners.data.local.entities.RouteEntity
import kotlinx.coroutines.flow.*
import org.osmdroid.util.GeoPoint
import javax.inject.Singleton

data class LiveRunState(
    val isTracking: Boolean = false,
    val pathPoints: List<GeoPoint> = emptyList(),
    val distanceMeters: Double = 0.0,
    val durationSeconds: Long = 0,
    val currentPace: Double = 0.0,
    val activeRoute: RouteEntity? = null,
    val ghostPosition: GeoPoint? = null,
    val timeDeltaSeconds: Long = 0,
    val currentHr: Int = 0,
    val currentHrZone: Int = 0,
    val zoneBreakdown: MutableList<Long> = mutableListOf(0L, 0L, 0L, 0L, 0L) // Seconds in Z1-Z5
)

@Singleton
object TrackingManager {
    private val _liveRunState = MutableStateFlow(LiveRunState())
    val liveRunState: StateFlow<LiveRunState> = _liveRunState.asStateFlow()

    private val _cheerEvent = MutableSharedFlow<String>()
    val cheerEvent: SharedFlow<String> = _cheerEvent.asSharedFlow()

    private var maxHr: Int = 190

    fun startRun(route: RouteEntity? = null, userMaxHr: Int = 190) {
        maxHr = userMaxHr
        _liveRunState.update { LiveRunState(isTracking = true, activeRoute = route) }
    }

    fun stopRun() {
        _liveRunState.update { it.copy(isTracking = false) }
    }

    fun resetRun() {
        _liveRunState.update { LiveRunState() }
    }

    fun receiveCheer(from: String) {
        _cheerEvent.tryEmit(from)
    }

    fun updateHeartRate(hr: Int) {
        _liveRunState.update { state ->
            val zone = calculateZone(hr, maxHr)
            state.copy(currentHr = hr, currentHrZone = zone)
        }
    }

    private fun calculateZone(hr: Int, max: Int): Int {
        val percent = (hr.toDouble() / max.toDouble()) * 100
        return when {
            percent < 60 -> 1
            percent < 70 -> 2
            percent < 80 -> 3
            percent < 90 -> 4
            else -> 5
        }
    }

    fun updateLocation(newPoint: GeoPoint, speed: Float) {
        _liveRunState.update { state ->
            val newPoints = state.pathPoints + newPoint
            var newDistance = state.distanceMeters
            if (state.pathPoints.isNotEmpty()) {
                val lastPoint = state.pathPoints.last()
                newDistance += lastPoint.distanceToAsDouble(newPoint)
            }
            
            val newDuration = state.durationSeconds
            val pace = if (newDistance > 0) (newDuration / 60.0) / (newDistance / 1000.0) else 0.0

            val ghostPos = state.activeRoute?.let { route ->
                GhostManager.calculateGhostPosition(route.pathPoints, route.bestDurationSeconds, newDuration)
            }

            state.copy(
                pathPoints = newPoints,
                distanceMeters = newDistance,
                currentPace = pace,
                ghostPosition = ghostPos
            )
        }
    }

    fun updateDuration(seconds: Long) {
        _liveRunState.update { state ->
            val pace = if (state.distanceMeters > 0) (seconds / 60.0) / (state.distanceMeters / 1000.0) else 0.0
            
            // Increment zone breakdown
            val newBreakdown = state.zoneBreakdown.toMutableList()
            if (state.currentHrZone in 1..5) {
                newBreakdown[state.currentHrZone - 1]++
            }

            val ghostPos = state.activeRoute?.let { route ->
                GhostManager.calculateGhostPosition(route.pathPoints, route.bestDurationSeconds, seconds)
            }

            state.copy(
                durationSeconds = seconds, 
                currentPace = pace,
                ghostPosition = ghostPos,
                zoneBreakdown = newBreakdown
            )
        }
    }
}

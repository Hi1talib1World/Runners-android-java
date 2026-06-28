package com.denzo.runners.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.osmdroid.util.GeoPoint
import javax.inject.Singleton

data class LiveRunState(
    val isTracking: Boolean = false,
    val pathPoints: List<GeoPoint> = emptyList(),
    val distanceMeters: Double = 0.0,
    val durationSeconds: Long = 0,
    val currentPace: Double = 0.0
)

@Singleton
object TrackingManager {
    private val _liveRunState = MutableStateFlow(LiveRunState())
    val liveRunState: StateFlow<LiveRunState> = _liveRunState.asStateFlow()

    fun startRun() {
        _liveRunState.update { LiveRunState(isTracking = true) }
    }

    fun stopRun() {
        _liveRunState.update { it.copy(isTracking = false) }
    }

    fun resetRun() {
        _liveRunState.update { LiveRunState() }
    }

    fun updateLocation(newPoint: GeoPoint, speed: Float) {
        _liveRunState.update { state ->
            val newPoints = state.pathPoints + newPoint
            var newDistance = state.distanceMeters
            if (state.pathPoints.isNotEmpty()) {
                val lastPoint = state.pathPoints.last()
                newDistance += lastPoint.distanceToAsDouble(newPoint)
            }
            
            val newDuration = state.durationSeconds // Duration is usually handled by a timer
            val pace = if (newDistance > 0) (newDuration / 60.0) / (newDistance / 1000.0) else 0.0

            state.copy(
                pathPoints = newPoints,
                distanceMeters = newDistance,
                currentPace = pace
            )
        }
    }

    fun updateDuration(seconds: Long) {
        _liveRunState.update { state ->
            val pace = if (state.distanceMeters > 0) (seconds / 60.0) / (state.distanceMeters / 1000.0) else 0.0
            state.copy(durationSeconds = seconds, currentPace = pace)
        }
    }
}

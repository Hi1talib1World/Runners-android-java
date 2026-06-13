package com.denzo.runners.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denzo.runners.data.local.entities.RunEntity
import com.denzo.runners.data.repository.RunRepository
import com.denzo.runners.features.subscription.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

data class HomeUiState(
    val isTracking: Boolean = false,
    val isLoading: Boolean = false,
    val isProUser: Boolean = false,
    val distanceMeters: Double = 0.0,
    val durationSeconds: Long = 0,
    val currentPace: Double = 0.0,
    val calories: Double = 0.0,
    val pathPoints: List<GeoPoint> = emptyList(),
    val error: String? = null,
    val currentActivity: ActivityMetrics = ActivityMetrics()
)

data class ActivityMetrics(
    val pace: String = "0'00''",
    val distance: String = "0.00",
    val duration: String = "00:00",
    val elevation: String = "0",
    val heartRate: String = "--",
    val cadence: String = "--"
)

sealed class UiEvent {
    object RunSaved : UiEvent()
    data class ShowError(val message: String) : UiEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: RunRepository,
    private val billingManager: BillingManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    private var trackingJob: Job? = null

    init {
        observeProStatus()
    }

    private fun observeProStatus() {
        viewModelScope.launch {
            billingManager.isProUser.collect { isPro ->
                _uiState.update { it.copy(isProUser = isPro) }
            }
        }
    }

    fun joinSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(1200) // Mock API latency
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun toggleTracking() {
        if (_uiState.value.isTracking) stopAndSaveRun() else startTracking()
    }

    private fun startTracking() {
        _uiState.update { it.copy(
            isTracking = true, 
            pathPoints = emptyList(), 
            distanceMeters = 0.0, 
            durationSeconds = 0,
            currentActivity = ActivityMetrics()
        ) }
        
        trackingJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                updateMetrics()
            }
        }
    }

    private fun updateMetrics() {
        _uiState.update { state ->
            val newDuration = state.durationSeconds + 1
            // Mocking coordinate movement
            val lastPoint = state.pathPoints.lastOrNull() ?: GeoPoint(48.8584, 2.2945)
            val newPoint = GeoPoint(lastPoint.latitude + 0.0001, lastPoint.longitude + 0.0001)
            
            val newDistance = state.distanceMeters + 10.5 // Simulated 10.5 meters per second
            val paceValue = if (newDistance > 0) (newDuration / 60.0) / (newDistance / 1000.0) else 0.0

            state.copy(
                durationSeconds = newDuration,
                distanceMeters = newDistance,
                currentPace = paceValue,
                calories = newDistance * 0.05,
                pathPoints = state.pathPoints + newPoint,
                currentActivity = state.currentActivity.copy(
                    duration = formatTime(newDuration.toInt()),
                    distance = String.format("%.2f", newDistance / 1000),
                    pace = String.format("%.2f", paceValue),
                    heartRate = (140..170).random().toString(),
                    cadence = (85..95).random().toString()
                )
            )
        }
    }

    private fun stopAndSaveRun() {
        trackingJob?.cancel()
        _uiState.update { it.copy(isLoading = true, isTracking = false) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentState = _uiState.value
                val runRecord = RunEntity(
                    timestamp = System.currentTimeMillis(),
                    avgPace = currentState.currentPace,
                    distanceMeters = currentState.distanceMeters,
                    durationSeconds = currentState.durationSeconds,
                    caloriesBurned = currentState.calories,
                    pathPoints = currentState.pathPoints
                )
                repository.saveRun(runRecord)
                _uiEvent.emit(UiEvent.RunSaved)
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Failed to save run: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isLoading = false, currentActivity = ActivityMetrics()) }
            }
        }
    }

    private fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val remainingSecs = seconds % 60
        return String.format("%02d:%02d", mins, remainingSecs)
    }
}

package com.denzo.runners.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denzo.runners.data.local.entities.Runningdata
import com.denzo.runners.data.remote.repository.ActivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val isLoading: Boolean = false,
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

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ActivityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    fun joinSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(1200) // Mock API latency
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun toggleTracking() {
        if (_uiState.value.isTracking) {
            stopTracking()
        } else {
            startTracking()
        }
    }

    private fun startTracking() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(1000) // Mock API latency for starting a session
            _uiState.update { it.copy(isTracking = true, isLoading = false, isPaused = false) }
            
            // Mock high-frequency data stream
            simulateMetricsStream()
        }
    }

    private fun stopTracking() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(800) // Mock API latency for final sync
            
            // Save to repository (Single Source of Truth)
            val finalData = Runningdata().apply {
                distance = _uiState.value.currentActivity.distance.toDoubleOrNull() ?: 0.0
                starttime = System.currentTimeMillis().toString()
            }
            repository.saveActivity(finalData)
            
            _uiState.update { it.copy(isTracking = false, isLoading = false, currentActivity = ActivityMetrics()) }
        }
    }

    private fun simulateMetricsStream() {
        viewModelScope.launch {
            var secs = 0
            var dist = 0.0
            while (_uiState.value.isTracking) {
                delay(1000)
                secs++
                dist += 0.002 // simulate movement
                
                _uiState.update { state ->
                    state.copy(
                        currentActivity = state.currentActivity.copy(
                            duration = formatTime(secs),
                            distance = String.format("%.2f", dist),
                            pace = "4'15''", // would be calculated based on speed
                            heartRate = (140..170).random().toString(),
                            cadence = (85..95).random().toString()
                        )
                    )
                }
            }
        }
    }

    private fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val remainingSecs = seconds % 60
        return String.format("%02d:%02d", mins, remainingSecs)
    }
}

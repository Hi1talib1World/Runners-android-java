package com.denzo.runners.features.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denzo.runners.BuildConfig
import com.denzo.runners.data.local.entities.RunEntity
import com.denzo.runners.data.repository.RunRepository
import com.denzo.runners.features.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


data class HistoryUiState(
    val isLoading: Boolean = false,
    val runs: List<RunEntity> = emptyList(),
    val isMetric: Boolean = true,
    val totalDistanceKm: Double = 0.0,
    val totalRuns: Int = 0,
    val errorEvent: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: RunRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _transientState = MutableStateFlow(HistoryUiState())

    // Mock data for debug mode
    private val mockRunsFlow = flowOf(
        if (BuildConfig.DEBUG) {
            listOf(
                RunEntity(id = 1, timestamp = System.currentTimeMillis() - 86400000, avgPace = 5.5, distanceMeters = 5000.0, durationSeconds = 1800, caloriesBurned = 400.0, pathPoints = emptyList(), zoneBreakdown = emptyList(), temperature = 22.0, humidity = 50.0, environment = "ROAD"),
                RunEntity(id = 2, timestamp = System.currentTimeMillis() - 172800000, avgPace = 6.0, distanceMeters = 3000.0, durationSeconds = 1200, caloriesBurned = 250.0, pathPoints = emptyList(), zoneBreakdown = emptyList(), temperature = 18.0, humidity = 60.0, environment = "TRAIL"),
                RunEntity(id = 3, timestamp = System.currentTimeMillis() - 259200000, avgPace = 5.0, distanceMeters = 10000.0, durationSeconds = 3600, caloriesBurned = 800.0, pathPoints = emptyList(), zoneBreakdown = emptyList(), temperature = 25.0, humidity = 40.0, environment = "ROAD")
            )
        } else {
            emptyList()
        }
    )

    // Combine persistent run data with settings and transient status
    val uiState: StateFlow<HistoryUiState> = combine(
        if (BuildConfig.DEBUG) mockRunsFlow else repository.getAllRuns(),
        settingsRepository.settingsFlow,
        _transientState
    ) { runs, settings, transient ->
        val totalDistMeters = runs.sumOf { it.distanceMeters }
        transient.copy(
            runs = runs.sortedByDescending { it.timestamp },
            isMetric = settings.isMetric,
            totalDistanceKm = totalDistMeters / 1000.0,
            totalRuns = runs.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState(isLoading = true)
    )

    fun deleteRun(run: RunEntity) {
        // Validation: Prevent deletion during sync or loading
        if (uiState.value.isLoading) {
            _transientState.update { it.copy(errorEvent = "System busy, try again later") }
            return
        }

        if (BuildConfig.DEBUG) {
            _transientState.update { it.copy(successMessage = "Debug: Mock run deleted") }
            return
        }

        executeAtomicAction(successMsg = "Activity deleted") {
            repository.deleteRun(run)
        }
    }

    fun clearHistory() {
        val currentState = uiState.value
        
        // Logical check: Is there anything to delete?
        if (currentState.runs.isEmpty()) {
            _transientState.update { it.copy(errorEvent = "Nothing to clear") }
            return
        }

        // Safety check: Prevent double-execution
        if (currentState.isLoading) return

        if (BuildConfig.DEBUG) {
            _transientState.update { it.copy(successMessage = "Debug: Mock history cleared") }
            return
        }

        executeAtomicAction(successMsg = "History cleared") {
            repository.clearAllHistory()
        }
    }


    private fun executeAtomicAction(
        successMsg: String? = null,
        action: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                _transientState.update { it.copy(isLoading = true, errorEvent = null, successMessage = null) }
                action()
                _transientState.update { it.copy(successMessage = successMsg) }
            } catch (e: Exception) {
                _transientState.update { it.copy(errorEvent = "Operation failed: ${e.message ?: "Unknown error"}") }
            } finally {
                _transientState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearError() {
        _transientState.update { it.copy(errorEvent = null) }
    }

    fun clearSuccess() {
        _transientState.update { it.copy(successMessage = null) }
    }
}

package com.denzo.runners.features.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denzo.runners.data.local.entities.RunEntity
import com.denzo.runners.data.repository.RunRepository
import com.denzo.runners.features.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pillar 1: Single Source of Truth for History
 */
data class HistoryUiState(
    val isLoading: Boolean = false,
    val runs: List<RunEntity> = emptyList(),
    val isMetric: Boolean = true,
    val totalDistance: Double = 0.0,
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

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.getAllRuns(),
        settingsRepository.settingsFlow,
        _transientState
    ) { runs, settings, transient ->
        val totalMeters = runs.sumOf { it.distanceMeters }
        val totalDistance = if (settings.isMetric) totalMeters / 1000.0 else totalMeters / 1609.34
        transient.copy(
            runs = runs,
            isMetric = settings.isMetric,
            totalDistance = totalDistance,
            totalRuns = runs.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    /**
     * Pillar 2: Atomic State Mutations
     */
    fun deleteRun(run: RunEntity) {
        executeAtomicAction(successMsg = "Run deleted") {
            repository.deleteRun(run)
        }
    }

    fun clearHistory() {
        if (uiState.value.runs.isEmpty()) return
        executeAtomicAction(successMsg = "History cleared") {
            repository.clearAllHistory()
        }
    }

    /**
     * Pillar 3 & 4: Micro-Feedback & Failure Safeguards
     */
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
                _transientState.update { it.copy(errorEvent = "Failed: ${e.message}") }
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

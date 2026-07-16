package com.denzo.runners.features.activities

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denzo.runners.data.local.entities.RouteEntity
import com.denzo.runners.data.local.entities.RunEntity
import com.denzo.runners.data.repository.RunRepository
import com.denzo.runners.features.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RunSummaryUiState(
    val isLoading: Boolean = false,
    val run: RunEntity? = null,
    val isMetric: Boolean = true,
    val successMessage: String? = null
)

@HiltViewModel
class RunSummaryViewModel @Inject constructor(
    private val repository: RunRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(RunSummaryUiState())
    val uiState: StateFlow<RunSummaryUiState> = _uiState.asStateFlow()

    init {
        val runId: Int? = savedStateHandle["runId"]
        if (runId != null && runId != -1) {
            loadRun(runId)
        }
    }

    private fun loadRun(runId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val run = repository.getRunById(runId)
            val settings = settingsRepository.settingsFlow.first()
            _uiState.update { it.copy(isLoading = false, run = run, isMetric = settings.isMetric) }
        }
    }

    fun deleteRun(run: RunEntity) {
        viewModelScope.launch {
            repository.deleteRun(run)
        }
    }

    fun saveAsRoute(name: String) {
        val run = _uiState.value.run ?: return
        viewModelScope.launch {
            val route = RouteEntity(
                name = name,
                distanceMeters = run.distanceMeters,
                bestDurationSeconds = run.durationSeconds,
                pathPoints = run.pathPoints,
                timestamp = System.currentTimeMillis()
            )
            repository.saveRoute(route)
            _uiState.update { it.copy(successMessage = "Route saved for Ghost Mode!") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

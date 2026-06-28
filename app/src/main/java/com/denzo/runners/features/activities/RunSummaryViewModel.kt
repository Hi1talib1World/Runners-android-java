package com.denzo.runners.features.activities

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
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RunSummaryUiState())
    val uiState = _uiState.asStateFlow()

    fun loadRun(runId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            combine(
                flow { emit(repository.getRunById(runId)) },
                settingsRepository.settingsFlow
            ) { run, settings ->
                _uiState.update { it.copy(
                    isLoading = false,
                    run = run,
                    isMetric = settings.isMetric
                ) }
            }.collect()
        }
    }

    fun saveAsRoute(name: String) {
        val run = _uiState.value.run ?: return
        viewModelScope.launch {
            val route = RouteEntity(
                name = name,
                distanceMeters = run.distanceMeters,
                bestDurationSeconds = run.durationSeconds,
                pathPoints = run.pathPoints
            )
            repository.saveRoute(route)
            _uiState.update { it.copy(successMessage = "Route saved successfully!") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

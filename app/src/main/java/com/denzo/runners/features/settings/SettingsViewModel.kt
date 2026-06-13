package com.denzo.runners.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isDarkMode: Boolean = true,
    val isTelemetryEnabled: Boolean = true,
    val isProcessing: Boolean = false,
    val errorEvent: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = combine(
        repository.settingsFlow,
        _uiState
    ) { persisted, transient ->
        transient.copy(
            isDarkMode = persisted.isDarkMode,
            isTelemetryEnabled = persisted.isTelemetryEnabled
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun toggleTheme(enabled: Boolean) {
        executeAtomicAction {
            repository.updateTheme(enabled)
            simulateCloudSync("Theme update")
        }
    }

    fun toggleTelemetry(enabled: Boolean) {
        executeAtomicAction {
            repository.updateTelemetry(enabled)
            simulateCloudSync("Telemetry config")
        }
    }

    private fun executeAtomicAction(action: suspend () -> Unit) {
        viewModelScope.launch {
            val previousState = _uiState.value
            try {
                _uiState.update { it.copy(isProcessing = true, errorEvent = null) }
                action()
            } catch (e: Exception) {
                // Transaction Safeguard: Rollback handled by SSOT observation
                // but we notify user of the failure
                _uiState.update { it.copy(errorEvent = "Sync failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    private suspend fun simulateCloudSync(operation: String) {
        // Pillar 2: Simulate remote cloud syncing with realistic delay
        delay(1000) 
        // Logic would go here to push to Firebase/API
    }

    fun clearError() {
        _uiState.update { it.copy(errorEvent = null) }
    }
}

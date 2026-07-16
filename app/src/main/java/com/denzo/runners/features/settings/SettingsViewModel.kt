package com.denzo.runners.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denzo.runners.data.repository.AuthRepository
import com.denzo.runners.data.repository.RunRepository
import com.denzo.runners.services.TrackingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isDarkMode: Boolean = true,
    val isMetric: Boolean = true,
    val isTelemetryEnabled: Boolean = true,
    val isSocialNotificationsEnabled: Boolean = true,
    val maxHeartRate: Int = 190,
    val syncFrequencyMinutes: Int = 15,
    val username: String = "",
    val isProcessing: Boolean = false,
    val errorEvent: String? = null,
    val successMessage: String? = null,
    val isLoggedOut: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val runRepository: RunRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _transientState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.settingsFlow,
        authRepository.displayName,
        _transientState
    ) { settings, authName, transient ->
        transient.copy(
            isDarkMode = settings.isDarkMode,
            isMetric = settings.isMetric,
            isTelemetryEnabled = settings.isTelemetryEnabled,
            isSocialNotificationsEnabled = settings.isSocialNotificationsEnabled,
            maxHeartRate = settings.maxHeartRate,
            syncFrequencyMinutes = settings.syncFrequencyMinutes,
            username = authName ?: "Unknown Runner"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun toggleTheme(isDark: Boolean) {
        executeAtomicAction("Theme updated") {
            repository.updateTheme(isDark)
            simulateCloudSync()
        }
    }

    fun toggleTelemetry(enabled: Boolean) {
        executeAtomicAction("Telemetry status changed") {
            repository.updateTelemetry(enabled)
        }
    }

    fun toggleUnitSystem(isMetric: Boolean) {
        executeAtomicAction("Units updated") {
            repository.updateUnitSystem(isMetric)
        }
    }

    fun toggleSocialNotifications(enabled: Boolean) {
        executeAtomicAction("Social alerts updated") {
            repository.updateSocialNotifications(enabled)
        }
    }

    fun updateSyncFrequency(minutes: Int) {
        executeAtomicAction("Sync window updated") {
            repository.updateSyncFrequency(minutes)
        }
    }

    fun updateUsername(newName: String) {
        if (newName.isBlank()) {
            _transientState.update { it.copy(errorEvent = "Username cannot be empty") }
            return
        }
        if (newName.length < 3) {
            _transientState.update { it.copy(errorEvent = "Username too short (min 3 chars)") }
            return
        }
        if (!newName.all { it.isLetterOrDigit() || it == '_' }) {
            _transientState.update { it.copy(errorEvent = "Only letters, digits, and underscores allowed") }
            return
        }

        executeAtomicAction("Profile synced to cloud") {
            authRepository.updateDisplayName(newName)
        }
    }

    fun updateMaxHr(hr: Int) {
        if (hr < 100) {
            _transientState.update { it.copy(errorEvent = "Max HR must be at least 100 BPM") }
            return
        } else if (hr > 230) {
            _transientState.update { it.copy(errorEvent = "Max HR exceeded physiological safety (max 230)") }
            return
        }

        executeAtomicAction("Telemetry refined") {
            repository.updateMaxHr(hr)
        }
    }

    fun clearLocalData() {
        executeAtomicAction("Local database purged") {
            runRepository.clearAllHistory()
        }
    }

    fun logout() {
        if (TrackingManager.liveRunState.value.isTracking) {
            _transientState.update { it.copy(errorEvent = "Cannot logout while tracking is active. Stop run first.") }
            return
        }

        executeAtomicAction("Session terminated safely") {
            authRepository.logout()
            _transientState.update { it.copy(isLoggedOut = true) }
        }
    }

    private fun executeAtomicAction(
        successMsg: String,
        action: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                _transientState.update { it.copy(isProcessing = true, errorEvent = null, successMessage = null) }
                action()
                _transientState.update { it.copy(successMessage = successMsg) }
            } catch (e: Exception) {
                _transientState.update { it.copy(errorEvent = "System sync failed: ${e.message}") }
            } finally {
                _transientState.update { it.copy(isProcessing = false) }
            }
        }
    }

    private suspend fun simulateCloudSync() {
        delay(800)
    }

    fun clearError() {
        _transientState.update { it.copy(errorEvent = null) }
    }

    fun clearSuccess() {
        _transientState.update { it.copy(successMessage = null) }
    }
}

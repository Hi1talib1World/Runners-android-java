package com.denzo.runners.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denzo.runners.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pillar 1: Single Source of Truth for UI State
 */
data class SettingsUiState(
    val isDarkMode: Boolean = true,
    val isMetric: Boolean = true,
    val isTelemetryEnabled: Boolean = true,
    val isSocialNotificationsEnabled: Boolean = true,
    val maxHeartRate: Int = 190,
    val syncFrequencyMinutes: Int = 15,
    val username: String = "Runner",
    val isProcessing: Boolean = false,
    val errorEvent: String? = null,
    val successMessage: String? = null,
    val isLoggedOut: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _transientState = MutableStateFlow(SettingsUiState())
    
    // Combine persistent repository state with transient UI state
    val uiState: StateFlow<SettingsUiState> = combine(
        repository.settingsFlow,
        _transientState
    ) { persisted, transient ->
        transient.copy(
            isDarkMode = persisted.isDarkMode,
            isMetric = persisted.isMetric,
            isTelemetryEnabled = persisted.isTelemetryEnabled,
            isSocialNotificationsEnabled = persisted.isSocialNotificationsEnabled,
            maxHeartRate = persisted.maxHeartRate,
            syncFrequencyMinutes = persisted.syncFrequencyMinutes,
            username = auth.currentUser?.displayName ?: "Runner"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    /**
     * Pillar 2: Atomic State Mutations
     */
    fun toggleTheme(enabled: Boolean) {
        executeAtomicAction("Syncing Theme...") {
            repository.updateTheme(enabled)
            simulateCloudSync()
        }
    }

    fun toggleTelemetry(enabled: Boolean) {
        executeAtomicAction("Updating Telemetry...") {
            repository.updateTelemetry(enabled)
            simulateCloudSync()
        }
    }

    fun toggleUnitSystem(isMetric: Boolean) {
        executeAtomicAction("Updating Unit System...") {
            repository.updateUnitSystem(isMetric)
            simulateCloudSync()
        }
    }

    fun toggleSocialNotifications(enabled: Boolean) {
        executeAtomicAction("Updating Social Alerts...") {
            repository.updateSocialNotifications(enabled)
            simulateCloudSync()
        }
    }

    fun updateSyncFrequency(minutes: Int) {
        executeAtomicAction("Adjusting Sync Frequency...") {
            repository.updateSyncFrequency(minutes)
            simulateCloudSync()
        }
    }

    fun updateUsername(newUsername: String) {
        if (newUsername.isBlank()) return
        executeAtomicAction("Updating Profile...") {
            _transientState.update { it.copy(username = newUsername) }
            simulateCloudSync()
        }
    }

    fun updateMaxHr(maxHr: Int) {
        if (maxHr < 100) return
        executeAtomicAction("Updating Bio Data...") {
            repository.updateMaxHr(maxHr)
            simulateCloudSync()
        }
    }

    fun logout() {
        executeAtomicAction("Terminating Session...") {
            auth.signOut()
            _transientState.update { it.copy(isLoggedOut = true, successMessage = "Logged out successfully") }
        }
    }

    /**
     * Pillar 3 & 4: Micro-Feedback & Failure Safeguards
     */
    private fun executeAtomicAction(
        logMessage: String,
        action: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                _transientState.update { it.copy(isProcessing = true, errorEvent = null, successMessage = null) }
                action()
                _transientState.update { it.copy(successMessage = "Changes saved successfully") }
            } catch (e: Exception) {
                _transientState.update { it.copy(errorEvent = "Failed: ${e.message ?: "Unknown error"}") }
            } finally {
                _transientState.update { it.copy(isProcessing = false) }
            }
        }
    }

    private suspend fun simulateCloudSync() {
        if (BuildConfig.DEBUG) {
            delay(300)
        } else {
            delay(1000)
        }
    }

    fun clearError() {
        _transientState.update { it.copy(errorEvent = null) }
    }

    fun clearSuccess() {
        _transientState.update { it.copy(successMessage = null) }
    }
}

package com.denzo.runners.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denzo.runners.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val currentStepIndex: Int = 0,
    val steps: List<OnboardingStep> = emptyList(),
    val isTransitioning: Boolean = false,
    val isCompleted: Boolean = false
) {
    val currentStep: OnboardingStep? get() = steps.getOrNull(currentStepIndex)
    val isLastStep: Boolean get() = currentStepIndex == steps.size - 1
    val totalSteps: Int get() = steps.size
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: OnboardingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadSteps()
    }

    private fun loadSteps() {
        // Pillar 4: Structural Data Strategy
        val steps = listOf(
            OnboardingStep(
                "Track Your Pace",
                "Real-time GPS tracking for your runs with precision accuracy.",
                android.R.drawable.ic_menu_mylocation
            ),
            OnboardingStep(
                "Analyze Performance",
                "Deep dive into your stats with comprehensive charts and metrics.",
                android.R.drawable.ic_menu_compass
            ),
            OnboardingStep(
                "Stay Motivated",
                "Join challenges and reach your fitness goals with the community.",
                android.R.drawable.ic_menu_gallery
            )
        )
        _uiState.update { it.copy(steps = steps) }
    }

    /**
     * Pillar 2: Managed State primitives & Navigation logic
     * Pillar 3: Active Navigation Guards
     */
    fun onNextClicked() {
        if (_uiState.value.isTransitioning) return

        viewModelScope.launch {
            if (_uiState.value.isLastStep) {
                completeOnboarding()
            } else {
                performTransition {
                    _uiState.update { it.copy(currentStepIndex = it.currentStepIndex + 1) }
                }
            }
        }
    }

    fun onBackClicked() {
        if (_uiState.value.isTransitioning || _uiState.value.currentStepIndex == 0) return

        viewModelScope.launch {
            performTransition {
                _uiState.update { it.copy(currentStepIndex = it.currentStepIndex - 1) }
            }
        }
    }

    fun onSkipClicked() {
        if (_uiState.value.isTransitioning) return
        viewModelScope.launch {
            completeOnboarding()
        }
    }

    private suspend fun performTransition(action: () -> Unit) {
        _uiState.update { it.copy(isTransitioning = true) }
        delay(400) // Pillar 3: Simulated 400ms transition channel
        action()
        _uiState.update { it.copy(isTransitioning = false) }
    }

    private suspend fun completeOnboarding() {
        _uiState.update { it.copy(isTransitioning = true) }
        // Pillar 1: Permanent Dismissal
        repository.setFirstRunCompleted()
        delay(300)
        _uiState.update { it.copy(isCompleted = true, isTransitioning = false) }
    }
}

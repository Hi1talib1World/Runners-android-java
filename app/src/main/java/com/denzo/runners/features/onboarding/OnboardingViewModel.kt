package com.denzo.runners.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denzo.runners.BuildConfig
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
        val steps = listOf(
            OnboardingStep(
                R.string.onboarding_title_1,
                R.string.onboarding_desc_1,
                android.R.drawable.ic_menu_mylocation
            ),
            OnboardingStep(
                R.string.onboarding_title_2,
                R.string.onboarding_desc_2,
                android.R.drawable.ic_menu_compass
            ),
            OnboardingStep(
                R.string.onboarding_title_3,
                R.string.onboarding_desc_3,
                android.R.drawable.ic_menu_gallery
            )
        )
        _uiState.update { it.copy(steps = steps) }
    }

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
        if (BuildConfig.DEBUG) {
            delay(100)
        } else {
            delay(400)
        }
        action()
        _uiState.update { it.copy(isTransitioning = false) }
    }

    private suspend fun completeOnboarding() {
        _uiState.update { it.copy(isTransitioning = true) }
        repository.setFirstRunCompleted()
        if (BuildConfig.DEBUG) {
            delay(50)
        } else {
            delay(300)
        }
        _uiState.update { it.copy(isCompleted = true, isTransitioning = false) }
    }
}

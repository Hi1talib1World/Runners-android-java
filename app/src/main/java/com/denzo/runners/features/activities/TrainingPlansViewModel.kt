package com.denzo.runners.features.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denzo.runners.data.local.entities.TrainingPlanEntity
import com.denzo.runners.data.local.entities.WorkoutEntity
import com.denzo.runners.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrainingPlansUiState(
    val isLoading: Boolean = false,
    val plans: List<TrainingPlanEntity> = emptyList(),
    val activePlan: TrainingPlanEntity? = null,
    val workouts: List<WorkoutEntity> = emptyList(),
    val customWorkouts: List<WorkoutEntity> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class TrainingPlansViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainingPlansUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observePlans()
        observeActivePlan()
        observeCustomWorkouts()
    }

    private fun observePlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getAllPlans().collect { list ->
                _uiState.update { it.copy(plans = list, isLoading = false) }
            }
        }
    }

    private fun observeActivePlan() {
        viewModelScope.launch {
            repository.getActivePlan().collect { plan ->
                _uiState.update { it.copy(activePlan = plan) }
                if (plan != null) {
                    loadWorkouts(plan.id)
                }
            }
        }
    }

    private fun observeCustomWorkouts() {
        viewModelScope.launch {
            repository.getWorkoutsForPlan(-1).collect { list ->
                _uiState.update { it.copy(customWorkouts = list) }
            }
        }
    }

    private fun loadWorkouts(planId: Int) {
        viewModelScope.launch {
            repository.getWorkoutsForPlan(planId).collect { list ->
                _uiState.update { it.copy(workouts = list) }
            }
        }
    }

    fun onEnroll(planId: Int) {
        viewModelScope.launch {
            repository.enrollInPlan(planId)
        }
    }
}

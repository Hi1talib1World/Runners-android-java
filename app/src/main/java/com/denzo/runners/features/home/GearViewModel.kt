package com.denzo.runners.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denzo.runners.data.local.entities.GearEntity
import com.denzo.runners.data.repository.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GearUiState(
    val isLoading: Boolean = false,
    val gearList: List<GearEntity> = emptyList(),
    val isMetric: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class GearViewModel @Inject constructor(
    private val repository: RunRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GearUiState())
    val uiState: StateFlow<GearUiState> = _uiState.asStateFlow()

    init {
        loadGear()
    }

    private fun loadGear() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getAllGear().collect { list ->
                _uiState.update { it.copy(isLoading = false, gearList = list) }
            }
        }
    }

    fun onAddGear(brand: String, model: String, maxKm: Double) {
        viewModelScope.launch {
            val gear = GearEntity(
                brand = brand,
                model = model,
                maxMileageMeters = maxKm * 1000.0,
                isActive = _uiState.value.gearList.none { it.isActive }
            )
            repository.addGear(gear)
        }
    }

    fun onSetActive(gear: GearEntity) {
        viewModelScope.launch {
            repository.setActiveGear(gear.id)
        }
    }

    fun onRetire(gear: GearEntity) {
        viewModelScope.launch {
            repository.updateGear(gear.copy(isActive = false, isRetired = true))
        }
    }
}

package com.denzo.runners.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denzo.runners.data.local.entities.GearEntity
import com.denzo.runners.data.repository.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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

    fun loadGear() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getAllGear().collect { list ->
                _uiState.update { it.copy(gearList = list, isLoading = false) }
            }
        }
    }

    fun onAddGear(brand: String, model: String, maxMeters: Double) {
        viewModelScope.launch {
            val gear = GearEntity(brand = brand, model = model, maxMileageMeters = maxMeters)
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

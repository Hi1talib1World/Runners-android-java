package com.denzo.runners.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val name: String = "Elite Runner",
    val memberSince: String = "Jan 2023",
    val isPro: Boolean = true,
    val records: List<RunRecord> = emptyList(),
    val gear: GearInfo? = null
)

data class RunRecord(val label: String, val value: String)
data class GearInfo(val model: String, val currentKm: Int, val limitKm: Int)

@HiltViewModel
class ProfileViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(1000)
            
            _uiState.update { it.copy(
                isLoading = false,
                records = listOf(
                    RunRecord("5K RECORD", "18:42"),
                    RunRecord("10K RECORD", "39:15"),
                    RunRecord("HALF MARATHON", "1:24:55"),
                    RunRecord("FULL MARATHON", "2:58:12")
                ),
                gear = GearInfo("NIKE ALPHAFLY 3", 412, 650)
            ) }
        }
    }

    fun retireGear() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(800)
            _uiState.update { it.copy(isLoading = false, gear = null) }
        }
    }
}

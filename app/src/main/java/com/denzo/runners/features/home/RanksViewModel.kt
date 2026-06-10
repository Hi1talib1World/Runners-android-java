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

data class AthleteRank(
    val rank: String,
    val name: String,
    val team: String,
    val distance: String,
    val isPro: Boolean = false,
    val isMe: Boolean = false
)

data class RanksUiState(
    val isLoading: Boolean = false,
    val podium: List<AthleteRank> = emptyList(),
    val rankings: List<AthleteRank> = emptyList()
)

@HiltViewModel
class RanksViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(RanksUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadRankings()
    }

    private fun loadRankings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(1500) // Mock API network delay
            
            val mockPodium = listOf(
                AthleteRank("02", "ELARA_V", "CITY SQUAD", "52.1 MI"),
                AthleteRank("01", "J_WOLFF", "VELOCITY ELITE", "61.4 MI", isPro = true),
                AthleteRank("03", "NIA.RUNS", "INDEPENDENT", "48.9 MI")
            )
            
            val mockRankings = listOf(
                AthleteRank("04", "Marcus Thorne", "VELOCITY ELITE", "48.2 MI", isPro = true),
                AthleteRank("05", "Alex Chen (You)", "INDEPENDENT", "45.8 MI", isMe = true),
                AthleteRank("06", "Sarah Miller", "TRAILBLAZERS", "42.1 MI"),
                AthleteRank("07", "David K.", "CITY SQUAD", "39.5 MI", isPro = true),
                AthleteRank("08", "Jessica Wu", "VELOCITY ELITE", "37.2 MI")
            )
            
            _uiState.update { it.copy(
                isLoading = false,
                podium = mockPodium,
                rankings = mockRankings
            ) }
        }
    }
}

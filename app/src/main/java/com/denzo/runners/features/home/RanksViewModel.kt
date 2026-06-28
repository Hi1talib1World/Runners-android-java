package com.denzo.runners.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denzo.runners.data.local.dao.ChallengeDao
import com.denzo.runners.data.local.entities.ChallengeEntity
import com.denzo.runners.data.repository.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

enum class RanksTab { GLOBAL, CLUBS, FRIENDS, CHALLENGES }

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
    val selectedTab: RanksTab = RanksTab.GLOBAL,
    val podium: List<AthleteRank> = emptyList(),
    val rankings: List<AthleteRank> = emptyList(),
    val challenges: List<ChallengeEntity> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class RanksViewModel @Inject constructor(
    private val repository: RunRepository,
    private val challengeDao: ChallengeDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(RanksUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadRankings(RanksTab.GLOBAL)
        observeChallenges()
    }

    private fun observeChallenges() {
        viewModelScope.launch {
            challengeDao.getAllChallenges().collect { list ->
                _uiState.update { it.copy(challenges = list) }
            }
        }
    }

    fun onTabSelected(tab: RanksTab) {
        if (_uiState.value.selectedTab == tab) return
        _uiState.update { it.copy(selectedTab = tab) }
        if (tab != RanksTab.CHALLENGES) {
            loadRankings(tab)
        }
    }

    fun onJoinChallenge(challengeId: Int) {
        viewModelScope.launch {
            challengeDao.joinChallenge(challengeId)
        }
    }

    private fun loadRankings(tab: RanksTab) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val runs = repository.getAllRuns().first()
            val myDistanceKm = runs.sumOf { it.distanceMeters } / 1000.0
            
            delay(600) // Simulation delay

            val (podium, baseRankings) = when (tab) {
                RanksTab.GLOBAL -> getGlobalData()
                RanksTab.CLUBS -> getClubsData()
                RanksTab.FRIENDS -> getFriendsData()
                RanksTab.CHALLENGES -> Pair(emptyList(), emptyList()) // Handled by separate observer
            }
            
            val mockRankings = baseRankings.toMutableList()
            
            // Insert "Me" into rankings
            mockRankings.add(AthleteRank(
                "00", 
                "You", 
                if (tab == RanksTab.CLUBS) "CITY SQUAD" else "INDEPENDENT", 
                String.format(Locale.getDefault(), "%.1f KM", myDistanceKm), 
                isMe = true
            ))
            
            mockRankings.sortByDescending { it.distance.split(" ")[0].toDoubleOrNull() ?: 0.0 }
            
            // Re-rank based on distance
            val finalRankings = mockRankings.mapIndexed { index, athlete ->
                athlete.copy(rank = String.format(Locale.getDefault(), "%02d", index + 4))
            }

            _uiState.update { it.copy(
                isLoading = false,
                podium = podium,
                rankings = finalRankings
            ) }
        }
    }

    private fun getGlobalData() = Pair(
        listOf(
            AthleteRank("02", "ELARA_V", "CITY SQUAD", "52.1 KM"),
            AthleteRank("01", "J_WOLFF", "VELOCITY ELITE", "61.4 KM", isPro = true),
            AthleteRank("03", "NIA.RUNS", "INDEPENDENT", "48.9 KM")
        ),
        listOf(
            AthleteRank("04", "Marcus Thorne", "VELOCITY ELITE", "48.2 KM", isPro = true),
            AthleteRank("06", "Sarah Miller", "TRAILBLAZERS", "42.1 KM"),
            AthleteRank("07", "David K.", "CITY SQUAD", "39.5 KM", isPro = true),
            AthleteRank("08", "Jessica Wu", "VELOCITY ELITE", "37.2 KM")
        )
    )

    private fun getClubsData() = Pair(
        listOf(
            AthleteRank("02", "S_CHEN", "CITY SQUAD", "45.2 KM"),
            AthleteRank("01", "MIKE_RUNS", "CITY SQUAD", "48.7 KM", isPro = true),
            AthleteRank("03", "LISA_M", "CITY SQUAD", "41.1 KM")
        ),
        listOf(
            AthleteRank("04", "Tom H.", "CITY SQUAD", "38.2 KM"),
            AthleteRank("06", "Anna G.", "CITY SQUAD", "35.5 KM", isPro = true),
            AthleteRank("07", "Chris P.", "CITY SQUAD", "32.1 KM")
        )
    )

    private fun getFriendsData() = Pair(
        listOf(
            AthleteRank("02", "Sarah Miller", "TRAILBLAZERS", "42.1 KM"),
            AthleteRank("01", "David K.", "CITY SQUAD", "44.5 KM", isPro = true),
            AthleteRank("03", "Alex J.", "INDEPENDENT", "38.9 KM")
        ),
        listOf(
            AthleteRank("04", "Ben S.", "INDEPENDENT", "35.2 KM"),
            AthleteRank("06", "Chloe W.", "VELOCITY ELITE", "31.4 KM")
        )
    )
}

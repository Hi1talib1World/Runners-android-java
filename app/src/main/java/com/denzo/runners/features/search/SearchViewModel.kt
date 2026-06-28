package com.denzo.runners.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchResult(
    val id: String,
    val name: String,
    val description: String,
    val type: SearchResultType,
    val isActionTaken: Boolean = false
)

enum class SearchResultType { ATHLETE, CLUB }

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val featured: List<SearchResult> = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private val allAthletes = listOf(
        SearchResult("1", "Marcus Thorne", "Elite Marathoner", SearchResultType.ATHLETE),
        SearchResult("2", "Sarah Miller", "Trail Runner", SearchResultType.ATHLETE),
        SearchResult("3", "David K.", "City Squad Member", SearchResultType.ATHLETE),
        SearchResult("4", "Jessica Wu", "Velocity Elite", SearchResultType.ATHLETE)
    )

    private val allClubs = listOf(
        SearchResult("c1", "CITY SQUAD", "Urban running community", SearchResultType.CLUB),
        SearchResult("c2", "VELOCITY ELITE", "Competitive racing club", SearchResultType.CLUB),
        SearchResult("c3", "TRAILBLAZERS", "Off-road & mountain runners", SearchResultType.CLUB),
        SearchResult("c4", "INDEPENDENT RUNNERS", "Join for solo motivation", SearchResultType.CLUB)
    )

    init {
        loadFeatured()
    }

    private fun loadFeatured() {
        _uiState.update { it.copy(
            featured = listOf(allClubs[0], allAthletes[0], allClubs[2])
        ) }
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        if (newQuery.length >= 2) {
            performSearch(newQuery)
        } else {
            _uiState.update { it.copy(results = emptyList(), isLoading = false) }
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(500) // Mock network delay
            
            val filtered = (allAthletes + allClubs).filter {
                it.name.contains(query, ignoreCase = true) || 
                it.description.contains(query, ignoreCase = true)
            }
            
            _uiState.update { it.copy(results = filtered, isLoading = false) }
        }
    }

    fun onActionClicked(result: SearchResult) {
        // Toggle follow/join state
        _uiState.update { state ->
            val updatedResults = state.results.map {
                if (it.id == result.id) it.copy(isActionTaken = !it.isActionTaken) else it
            }
            val updatedFeatured = state.featured.map {
                if (it.id == result.id) it.copy(isActionTaken = !it.isActionTaken) else it
            }
            state.copy(results = updatedResults, featured = updatedFeatured)
        }
    }
}

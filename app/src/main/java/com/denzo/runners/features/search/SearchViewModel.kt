package com.denzo.runners.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val allAthletes = listOf(
        SearchResult("1", "Marcus Thorne", "Level 42 Athlete", SearchResultType.ATHLETE),
        SearchResult("2", "Sarah Miller", "Marathon Finisher", SearchResultType.ATHLETE),
        SearchResult("3", "David K.", "Trail Running Specialist", SearchResultType.ATHLETE)
    )

    private val allClubs = listOf(
        SearchResult("c1", "Velocity Elite", "High-performance running club", SearchResultType.CLUB),
        SearchResult("c2", "City Squad", "Community morning runs", SearchResultType.CLUB)
    )

    private var searchJob: Job? = null

    init {
        loadFeatured()
    }

    private fun loadFeatured() {
        _uiState.update { it.copy(featured = allAthletes.take(2)) }
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _uiState.update { it.copy(results = emptyList(), isLoading = false) }
        } else {
            searchJob = viewModelScope.launch {
                delay(300)
                performSearch(newQuery)
            }
        }
    }

    private fun performSearch(q: String) {
        _uiState.update { it.copy(isLoading = true) }
        val filtered = (allAthletes + allClubs).filter { 
            it.name.contains(q, ignoreCase = true) || it.description.contains(q, ignoreCase = true)
        }
        _uiState.update { it.copy(isLoading = false, results = filtered) }
    }

    fun onActionClicked(result: SearchResult) {
        _uiState.update { state ->
            val newResults = state.results.map { 
                if (it.id == result.id) it.copy(isActionTaken = !it.isActionTaken) else it 
            }
            state.copy(results = newResults)
        }
    }
}

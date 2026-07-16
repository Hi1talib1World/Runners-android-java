package com.denzo.runners.features.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denzo.runners.data.repository.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedActivity(
    val id: String,
    val athleteName: String,
    val distanceKm: String,
    val duration: String,
    val kudosCount: Int,
    val isLive: Boolean = false
)

data class FeedUiState(
    val isLoading: Boolean = false,
    val feed: List<FeedActivity> = emptyList()
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: RunRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        loadFeed()
    }

    fun loadFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(800) // Simulation
            val mockFeed = listOf(
                FeedActivity("f1", "Marcus Thorne", "12.5", "01:05:22", 12, isLive = true),
                FeedActivity("f2", "Sarah Miller", "5.2", "00:26:45", 8),
                FeedActivity("f3", "David K.", "21.1", "01:45:10", 24)
            )
            _uiState.update { it.copy(isLoading = false, feed = mockFeed) }
        }
    }

    fun onKudos(activityId: String) {
        _uiState.update { state ->
            val newFeed = state.feed.map { 
                if (it.id == activityId) it.copy(kudosCount = it.kudosCount + 1) else it 
            }
            state.copy(feed = newFeed)
        }
    }
}

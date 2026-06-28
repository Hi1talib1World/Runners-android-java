package com.denzo.runners.features.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denzo.runners.data.repository.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedActivity(
    val id: String,
    val athleteName: String,
    val timestamp: String,
    val title: String,
    val distance: String,
    val pace: String,
    val duration: String,
    val kudosCount: Int,
    val commentCount: Int,
    val isKudoed: Boolean = false,
    val isLive: Boolean = false
)

data class FeedUiState(
    val isLoading: Boolean = false,
    val activities: List<FeedActivity> = emptyList()
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: RunRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadFeed()
    }

    fun loadFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(800) // Simulation
            
            val mockFeed = listOf(
                FeedActivity("1", "Marcus Thorne", "LIVE", "Morning Tempo Run", "12.4 km", "4'12'' /km", "52:10", 24, 3, isLive = true),
                FeedActivity("2", "Sarah Miller", "4h ago", "Trail Exploration", "8.2 km", "5'45'' /km", "45:30", 15, 1),
                FeedActivity("3", "David K.", "6h ago", "Easy Recovery", "5.0 km", "6'02'' /km", "30:10", 8, 0)
            )
            
            _uiState.update { it.copy(isLoading = false, activities = mockFeed) }
        }
    }

    fun toggleKudos(activityId: String) {
        _uiState.update { state ->
            val updated = state.activities.map {
                if (it.id == activityId) {
                    val newKudoed = !it.isKudoed
                    it.copy(
                        isKudoed = newKudoed,
                        kudosCount = if (newKudoed) it.kudosCount + 1 else it.kudosCount - 1
                    )
                } else it
            }
            state.copy(activities = updated)
        }
    }
}

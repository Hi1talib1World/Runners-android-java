package com.denzo.runners.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denzo.runners.core.analytics.Achievement
import com.denzo.runners.core.analytics.AchievementManager
import com.denzo.runners.core.utils.UnitConverter
import com.denzo.runners.data.local.entities.GearEntity
import com.denzo.runners.data.local.entities.RunEntity
import com.denzo.runners.data.repository.AuthRepository
import com.denzo.runners.data.repository.RunRepository
import com.denzo.runners.features.settings.SettingsRepository
import com.denzo.runners.features.subscription.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.pow

data class RacePrediction(val distance: String, val time: String)

/**
 * Pillar 1: Single Source of Truth for Profile
 */
data class ProfileUiState(
    val isLoading: Boolean = false,
    val name: String = "Runner",
    val email: String = "--",
    val memberSince: String = "--",
    val isPro: Boolean = false,
    val isMetric: Boolean = true,
    val records: List<RunRecord> = emptyList(),
    val gear: GearEntity? = null,
    val errorEvent: String? = null,
    val successMessage: String? = null,
    val lifetimeDistanceKm: String = "0.00",
    val totalRuns: Int = 0,
    val achievements: List<Achievement> = emptyList(),
    val predictions: List<RacePrediction> = emptyList(),
    val trainingLoad: Int = 0, // Monthly effort score
    val loadStatus: String = "OPTIMAL",
    val gearWearStatus: String? = null
)

data class RunRecord(val label: String, val value: String)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: RunRepository,
    private val achievementManager: AchievementManager,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val billingManager: BillingManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadProfile()
        observeProStatus()
    }

    private fun observeProStatus() {
        viewModelScope.launch {
            billingManager.isProUser.collect { isPro ->
                _uiState.update { it.copy(isPro = isPro) }
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            combine(
                repository.getAllRuns(),
                repository.getAllGear(),
                settingsRepository.settingsFlow,
                authRepository.displayName
            ) { runs, gearList, settings, name ->
                val email = authRepository.currentUserEmail
                val totalDistance = runs.sumOf { it.distanceMeters }
                val runCount = runs.size
                
                val achievements = achievementManager.calculateAchievements(runs)
                val activeGear = gearList.find { it.isActive }
                
                // If/Else Logic: Gear Wear Detection
                val gearStatus = if (activeGear != null && activeGear.maxMileageMeters > 0) {
                    val wearPercent = (activeGear.currentMileageMeters / activeGear.maxMileageMeters) * 100
                    if (wearPercent > 90) "REPLACE SOON"
                    else if (wearPercent > 75) "MODERATE WEAR"
                    else "GOOD CONDITION"
                } else null

                val records = mutableListOf<RunRecord>()
                runs.maxByOrNull { it.distanceMeters }?.let {
                    records.add(RunRecord("LONGEST RUN", UnitConverter.formatDistance(it.distanceMeters, settings.isMetric)))
                }
                runs.minByOrNull { it.avgPace }?.let {
                    if (it.avgPace > 0) {
                        records.add(RunRecord("BEST PACE", UnitConverter.formatPace(it.avgPace, settings.isMetric)))
                    }
                }

                val predictions = calculatePredictions(runs)
                val trainingLoad = calculateTrainingLoad(runs)
                
                // If/Else Logic: Load Status Classification
                val loadStatusText = if (trainingLoad > 500) "OVERREACHING"
                else if (trainingLoad > 300) "OPTIMAL"
                else if (trainingLoad > 100) "MAINTAINING"
                else "RECOVERY"

                _uiState.update { it.copy(
                    name = name ?: "Runner",
                    email = email ?: "--",
                    isMetric = settings.isMetric,
                    records = records,
                    gear = activeGear,
                    lifetimeDistanceKm = UnitConverter.formatDistance(totalDistance, settings.isMetric).split(" ")[0],
                    totalRuns = runCount,
                    achievements = achievements,
                    predictions = predictions,
                    trainingLoad = trainingLoad,
                    loadStatus = loadStatusText,
                    gearWearStatus = gearStatus
                ) }
            }.collect()
        }
    }

    private fun calculatePredictions(runs: List<RunEntity>): List<RacePrediction> {
        val bestRun = runs.filter { it.distanceMeters >= 1000 }.minByOrNull { it.avgPace } ?: return emptyList()
        
        val t1 = bestRun.durationSeconds.toDouble()
        val d1 = bestRun.distanceMeters
        
        fun predict(d2: Double): String {
            val t2 = t1 * (d2 / d1).pow(1.06)
            return formatDuration(t2.toLong())
        }

        return listOf(
            RacePrediction("5K", predict(5000.0)),
            RacePrediction("10K", predict(10000.0)),
            RacePrediction("HALF", predict(21097.5))
        )
    }

    private fun calculateTrainingLoad(runs: List<RunEntity>): Int {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val recentRuns = runs.filter { it.timestamp > thirtyDaysAgo }
        
        var totalLoad = 0.0
        recentRuns.forEach { run ->
            if (run.zoneBreakdown.isNotEmpty()) {
                run.zoneBreakdown.forEachIndexed { index, seconds ->
                    totalLoad += (seconds / 60.0) * (index + 1)
                }
            } else {
                totalLoad += (run.durationSeconds / 60.0) * 2.0
            }
        }
        return totalLoad.toInt()
    }

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
    }

    fun retireGear() {
        val currentState = _uiState.value
        if (currentState.gear == null) {
            _uiState.update { it.copy(errorEvent = "No active gear to retire") }
            return
        }

        executeAtomicAction(successMsg = "Gear retired successfully") {
            repository.updateGear(currentState.gear.copy(isActive = false, isRetired = true))
        }
    }

    fun updateProfileName(newName: String) {
        if (newName.isBlank()) {
            _uiState.update { it.copy(errorEvent = "Name cannot be empty") }
            return
        }
        
        if (newName.length < 2) {
            _uiState.update { it.copy(errorEvent = "Name too short") }
            return
        }

        executeAtomicAction(successMsg = "Profile updated") {
            authRepository.updateDisplayName(newName).getOrThrow()
        }
    }

    fun goPro(activity: android.app.Activity) {
        if (_uiState.value.isPro) {
            _uiState.update { it.copy(successMessage = "You are already a Pro member!") }
            return
        }
        billingManager.purchasePro(activity)
    }

    private fun executeAtomicAction(
        successMsg: String? = null,
        action: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorEvent = null, successMessage = null) }
                action()
                _uiState.update { it.copy(successMessage = successMsg) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorEvent = "Operation failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorEvent = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

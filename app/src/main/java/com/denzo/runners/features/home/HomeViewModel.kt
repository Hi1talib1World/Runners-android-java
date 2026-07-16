package com.denzo.runners.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denzo.runners.core.health.HealthConnectManager
import com.denzo.runners.core.utils.DateTimeUtils
import com.denzo.runners.core.utils.UnitConverter
import com.denzo.runners.data.local.entities.RouteEntity
import com.denzo.runners.data.local.entities.RunEntity
import com.denzo.runners.data.local.entities.WorkoutEntity
import com.denzo.runners.data.local.entities.WorkoutStep
import com.denzo.runners.data.repository.RunRepository
import com.denzo.runners.data.repository.WorkoutRepository
import com.denzo.runners.features.settings.SettingsRepository
import com.denzo.runners.features.subscription.BillingManager
import com.denzo.runners.services.TrackingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.osmdroid.util.GeoPoint
import java.util.Locale
import javax.inject.Inject

enum class RunGoal { FREE, DISTANCE_5K, DISTANCE_10K, TIME_30M, TIME_60M, ROUTE, WORKOUT }

data class LiveAthlete(
    val id: String,
    val name: String,
    val position: GeoPoint,
    val pace: String
)

data class HomeUiState(
    val isTracking: Boolean = false,
    val isLoading: Boolean = false,
    val isProUser: Boolean = false,
    val isMetric: Boolean = true,
    val isLiveBroadcasting: Boolean = false,
    val isLiveGroupJoined: Boolean = false,
    val liveAthletes: List<LiveAthlete> = emptyList(),
    val selectedGoal: RunGoal = RunGoal.FREE,
    val selectedEnvironment: String = "ROAD",
    val selectedRoute: RouteEntity? = null,
    val availableRoutes: List<RouteEntity> = emptyList(),
    val todayWorkout: WorkoutEntity? = null,
    val activeWorkoutStep: WorkoutStep? = null,
    val stepRemainingSeconds: Long = 0,
    val goalProgress: Int = 0,
    val distanceMeters: Double = 0.0,
    val durationSeconds: Long = 0,
    val currentPace: Double = 0.0,
    val currentHr: Int = 0,
    val currentHrZone: Int = 0,
    val zoneBreakdown: List<Long> = emptyList(),
    val calories: Double = 0.0,
    val pathPoints: List<GeoPoint> = emptyList(),
    val ghostPosition: GeoPoint? = null,
    val error: String? = null,
    val currentActivity: ActivityMetrics = ActivityMetrics()
)

data class ActivityMetrics(
    val pace: String = "0'00''",
    val distance: String = "0.00",
    val duration: String = "00:00",
    val elevation: String = "0",
    val heartRate: String = "--",
    val cadence: String = "--"
)

sealed class UiEvent {
    object RunSaved : UiEvent()
    data class ShowError(val message: String) : UiEvent()
    data class ReceivedCheer(val from: String) : UiEvent()
    data class NewStep(val instruction: String) : UiEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: RunRepository,
    private val workoutRepository: WorkoutRepository,
    private val billingManager: BillingManager,
    private val settingsRepository: SettingsRepository,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    companion object {
        private const val MOCK_JOIN_DELAY = 1500L
        private const val ATHLETE_UPDATE_DELAY = 3000L
        
        private const val MET_ROAD = 0.062
        private const val MET_TRAIL = 0.085
        private const val MET_TREADMILL = 0.055

        private const val DISTANCE_5K_METERS = 5000.0
        private const val DISTANCE_10K_METERS = 10000.0
        private const val TIME_30M_SECONDS = 1800L
        private const val TIME_60M_SECONDS = 3600L
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    private var liveAthleteJob: Job? = null
    private var currentWorkoutSteps: List<WorkoutStep> = emptyList()
    private var currentStepIndex: Int = -1
    private var stepStartSeconds: Long = 0

    init {
        observeProStatus()
        observeSettings()
        observeLiveTracking()
        loadRoutes()
        loadTodayWorkout()
        
        viewModelScope.launch {
            TrackingManager.cheerEvent.collect { from ->
                _uiEvent.emit(UiEvent.ReceivedCheer(from))
            }
        }
    }

    private fun loadTodayWorkout() {
        viewModelScope.launch {
            workoutRepository.getActivePlan().collect { plan ->
                if (plan != null) {
                    workoutRepository.getNextWorkout(plan.id).collect { workout ->
                        _uiState.update { it.copy(todayWorkout = workout) }
                    }
                }
            }
        }
    }

    private fun loadRoutes() {
        viewModelScope.launch {
            repository.getAllRoutes().collect { routes ->
                _uiState.update { it.copy(availableRoutes = routes) }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(isMetric = settings.isMetric) }
            }
        }
    }

    private fun observeLiveTracking() {
        viewModelScope.launch {
            TrackingManager.liveRunState.collect { liveState ->
                _uiState.update { state ->
                    val progress = if (state.selectedGoal != RunGoal.FREE) {
                        calculateGoalProgress(liveState, state.selectedGoal)
                    } else {
                        0
                    }
                    
                    if (state.selectedGoal == RunGoal.WORKOUT) {
                        checkWorkoutProgression(liveState.durationSeconds)
                    }

                    state.copy(
                        isTracking = liveState.isTracking,
                        distanceMeters = liveState.distanceMeters,
                        durationSeconds = liveState.durationSeconds,
                        currentPace = liveState.currentPace,
                        currentHr = liveState.currentHr,
                        currentHrZone = liveState.currentHrZone,
                        zoneBreakdown = liveState.zoneBreakdown,
                        pathPoints = liveState.pathPoints,
                        ghostPosition = liveState.ghostPosition,
                        goalProgress = progress,
                        currentActivity = state.currentActivity.copy(
                            duration = DateTimeUtils.formatDuration(liveState.durationSeconds),
                            distance = UnitConverter.formatDistance(liveState.distanceMeters, state.isMetric).split(" ")[0],
                            pace = UnitConverter.formatPace(liveState.currentPace, state.isMetric).split(" ")[0]
                        )
                    )
                }
            }
        }
    }

    private fun checkWorkoutProgression(currentSeconds: Long) {
        val activeStep = _uiState.value.activeWorkoutStep ?: return
        val elapsedInStep = currentSeconds - stepStartSeconds
        val remaining = activeStep.durationSeconds - elapsedInStep

        if (remaining <= 0) {
            moveToNextStep(currentSeconds)
        } else {
            _uiState.update { it.copy(stepRemainingSeconds = remaining) }
        }
    }

    private fun moveToNextStep(currentSeconds: Long) {
        currentStepIndex++
        if (currentStepIndex < currentWorkoutSteps.size) {
            val nextStep = currentWorkoutSteps[currentStepIndex]
            stepStartSeconds = currentSeconds
            _uiState.update { it.copy(activeWorkoutStep = nextStep, stepRemainingSeconds = nextStep.durationSeconds) }
            viewModelScope.launch { _uiEvent.emit(UiEvent.NewStep(nextStep.instruction)) }
        } else {
            _uiState.update { it.copy(activeWorkoutStep = null, selectedGoal = RunGoal.FREE) }
        }
    }

    private fun calculateGoalProgress(liveState: com.denzo.runners.services.LiveRunState, goal: RunGoal): Int {
        return when (goal) {
            RunGoal.FREE, RunGoal.ROUTE, RunGoal.WORKOUT -> 0
            RunGoal.DISTANCE_5K -> ((liveState.distanceMeters / DISTANCE_5K_METERS) * 100).toInt().coerceIn(0, 100)
            RunGoal.DISTANCE_10K -> ((liveState.distanceMeters / DISTANCE_10K_METERS) * 100).toInt().coerceIn(0, 100)
            RunGoal.TIME_30M -> ((liveState.durationSeconds / TIME_30M_SECONDS.toDouble()) * 100).toInt().coerceIn(0, 100)
            RunGoal.TIME_60M -> ((liveState.durationSeconds / TIME_60M_SECONDS.toDouble()) * 100).toInt().coerceIn(0, 100)
        }
    }

    fun onGoalSelected(goal: RunGoal) {
        if (!_uiState.value.isTracking) {
            _uiState.update { it.copy(selectedGoal = goal, activeWorkoutStep = null) }
        }
    }

    fun onEnvironmentSelected(env: String) {
        if (!_uiState.value.isTracking) {
            _uiState.update { it.copy(selectedEnvironment = env) }
        }
    }

    fun onTodayWorkoutClicked() {
        if (!_uiState.value.isTracking) {
            val workout = _uiState.value.todayWorkout ?: return
            currentWorkoutSteps = workout.steps
            currentStepIndex = 0
            stepStartSeconds = 0
            _uiState.update { it.copy(
                selectedGoal = RunGoal.WORKOUT,
                activeWorkoutStep = currentWorkoutSteps[0],
                stepRemainingSeconds = currentWorkoutSteps[0].durationSeconds
            ) }
        }
    }

    fun onRouteSelected(route: RouteEntity?) {
        if (!_uiState.value.isTracking) {
            _uiState.update { it.copy(selectedRoute = route, selectedGoal = if (route != null) RunGoal.ROUTE else RunGoal.FREE) }
        }
    }

    private fun observeProStatus() {
        viewModelScope.launch {
            billingManager.isProUser.collect { isPro ->
                _uiState.update { it.copy(isProUser = isPro) }
            }
        }
    }

    fun onVisibilityChanged(isVisible: Boolean) {
        if (isVisible) {
            if (_uiState.value.isLiveGroupJoined) {
                startLiveAthleteSimulation()
            }
        } else {
            liveAthleteJob?.cancel()
        }
    }

    fun joinSession() {
        if (_uiState.value.isLiveGroupJoined) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(MOCK_JOIN_DELAY)
            
            _uiState.update { it.copy(
                isLoading = false, 
                isLiveGroupJoined = true,
                isLiveBroadcasting = true
            ) }
            
            startLiveAthleteSimulation()
        }
    }

    private fun startLiveAthleteSimulation() {
        liveAthleteJob?.cancel()
        liveAthleteJob = viewModelScope.launch {
            while (isActive) {
                val currentPos = _uiState.value.pathPoints.lastOrNull() ?: GeoPoint(31.5101, -9.7557)
                val mockAthletes = listOf(
                    LiveAthlete("a1", "Sarah M.", GeoPoint(currentPos.latitude + 0.0005, currentPos.longitude + 0.0005), "4'45''"),
                    LiveAthlete("a2", "Marcus T.", GeoPoint(currentPos.latitude - 0.0003, currentPos.longitude + 0.0008), "5'10''"),
                    LiveAthlete("a3", "David K.", GeoPoint(currentPos.latitude + 0.001, currentPos.longitude - 0.0002), "4'55''")
                )
                _uiState.update { it.copy(liveAthletes = mockAthletes) }
                delay(ATHLETE_UPDATE_DELAY)
            }
        }
    }

    fun sendCheer(athleteId: String) {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowError("Cheer sent!")) 
        }
    }

    fun discardRun() {
        _uiState.update { it.copy(isTracking = false, isLiveGroupJoined = false, activeWorkoutStep = null) }
        liveAthleteJob?.cancel()
        TrackingManager.stopRun()
    }

    fun stopAndSaveRun() {
        val currentState = _uiState.value
        _uiState.update { it.copy(isLoading = true, isTracking = false, isLiveGroupJoined = false, activeWorkoutStep = null) }
        liveAthleteJob?.cancel()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val temp = (12..28).random().toDouble()
                val hum = (40..70).random().toDouble()

                val metabolicFactor = when(currentState.selectedEnvironment) {
                    "TRAIL" -> MET_TRAIL
                    "TREADMILL" -> MET_TREADMILL
                    else -> MET_ROAD
                }

                val runRecord = RunEntity(
                    timestamp = System.currentTimeMillis(),
                    avgPace = currentState.currentPace,
                    distanceMeters = currentState.distanceMeters,
                    durationSeconds = currentState.durationSeconds,
                    caloriesBurned = currentState.distanceMeters * metabolicFactor,
                    pathPoints = currentState.pathPoints,
                    zoneBreakdown = currentState.zoneBreakdown,
                    temperature = temp,
                    humidity = hum,
                    environment = currentState.selectedEnvironment,
                    isSynced = false
                )
                
                if (runRecord.distanceMeters > 0) {
                    repository.saveRun(runRecord)
                    healthConnectManager.writeRunToHealthConnect(runRecord)
                }

                if (currentState.selectedGoal == RunGoal.WORKOUT) {
                    currentState.todayWorkout?.let { workoutRepository.completeWorkout(it) }
                }
                
                _uiEvent.emit(UiEvent.RunSaved)
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Failed to save run: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isLoading = false, currentActivity = ActivityMetrics()) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        liveAthleteJob?.cancel()
    }
}

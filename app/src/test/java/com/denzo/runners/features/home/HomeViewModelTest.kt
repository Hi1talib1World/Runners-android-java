package com.denzo.runners.features.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.denzo.runners.core.health.HealthConnectManager
import com.denzo.runners.data.local.entities.ConfigEntity
import com.denzo.runners.data.repository.RunRepository
import com.denzo.runners.data.repository.WorkoutRepository
import com.denzo.runners.features.settings.SettingsRepository
import com.denzo.runners.features.subscription.BillingManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: HomeViewModel
    private val repository = mockk<RunRepository>(relaxed = true)
    private val workoutRepository = mockk<WorkoutRepository>(relaxed = true)
    private val billingManager = mockk<BillingManager>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val healthConnectManager = mockk<HealthConnectManager>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { settingsRepository.settingsFlow } returns flowOf(ConfigEntity())
        every { billingManager.isProUser } returns MutableStateFlow(false)
        every { repository.getAllRoutes() } returns flowOf(emptyList())
        every { workoutRepository.getActivePlan() } returns flowOf(null)

        viewModel = HomeViewModel(
            repository,
            workoutRepository,
            billingManager,
            settingsRepository,
            healthConnectManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(false, state.isTracking)
            assertEquals(RunGoal.FREE, state.selectedGoal)
        }
    }

    @Test
    fun `onGoalSelected updates state`() = runTest {
        viewModel.onGoalSelected(RunGoal.DISTANCE_5K)
        
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(RunGoal.DISTANCE_5K, state.selectedGoal)
        }
    }

    @Test
    fun `onEnvironmentSelected updates state`() = runTest {
        viewModel.onEnvironmentSelected("TRAIL")
        
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("TRAIL", state.selectedEnvironment)
        }
    }

    @Test
    fun `joinSession updates loading and then joined state`() = runTest {
        viewModel.joinSession()
        
        viewModel.uiState.test {
            // Advance past initial state
            awaitItem() 
            
            // Loading state
            val loadingState = awaitItem()
            assertEquals(true, loadingState.isLoading)
            
            // Advance delay
            testDispatcher.scheduler.advanceTimeBy(2000)
            
            val joinedState = awaitItem()
            assertEquals(false, joinedState.isLoading)
            assertEquals(true, joinedState.isLiveGroupJoined)
        }
    }
}

package com.denzo.runners.services

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalCoroutinesApi::class)
class TrackingManagerTest {

    @Before
    fun setup() {
        TrackingManager.resetRun()
    }

    @Test
    fun `startRun sets tracking to true`() = runTest {
        TrackingManager.startRun()
        
        TrackingManager.liveRunState.test {
            val state = awaitItem()
            assertTrue(state.isTracking)
            assertEquals(0L, state.durationSeconds)
        }
    }

    @Test
    fun `tickDuration increments duration only when tracking`() = runTest {
        TrackingManager.startRun()
        TrackingManager.tickDuration()
        
        TrackingManager.liveRunState.test {
            assertEquals(1L, awaitItem().durationSeconds)
        }

        TrackingManager.pauseRun()
        TrackingManager.tickDuration()
        
        TrackingManager.liveRunState.test {
            assertEquals(1L, awaitItem().durationSeconds) // Should not increment
        }
    }

    @Test
    fun `updateHeartRate calculates correct zones`() = runTest {
        TrackingManager.startRun(userMaxHr = 200)
        
        // Zone 1: < 60% (120)
        TrackingManager.updateHeartRate(110)
        assertEquals(1, TrackingManager.liveRunState.value.currentHrZone)

        // Zone 3: 70-80% (140-160)
        TrackingManager.updateHeartRate(150)
        assertEquals(3, TrackingManager.liveRunState.value.currentHrZone)
        
        // Zone 5: >= 90% (180)
        TrackingManager.updateHeartRate(190)
        assertEquals(5, TrackingManager.liveRunState.value.currentHrZone)
    }

    @Test
    fun `updateLocation calculates distance and pace`() = runTest {
        TrackingManager.startRun()
        
        val p1 = GeoPoint(0.0, 0.0)
        val p2 = GeoPoint(0.0, 0.0001) // Approx 11.1 meters
        
        TrackingManager.updateLocation(p1, 5.0f, 5.0f) // Start
        TrackingManager.updateLocation(p2, 5.0f, 5.0f) // Move
        
        val state = TrackingManager.liveRunState.value
        assertTrue(state.distanceMeters > 11.0)
        assertEquals(2, state.pathPoints.size)
    }

    @Test
    fun `auto pause triggers when speed is low`() = runTest {
        TrackingManager.startRun()
        
        // Advance duration to bypass grace period
        repeat(10) { TrackingManager.tickDuration() }
        
        TrackingManager.updateLocation(GeoPoint(0.0, 0.0), 0.1f, 5.0f)
        
        val state = TrackingManager.liveRunState.value
        assertFalse(state.isTracking)
        assertTrue(state.isAutoPaused)
    }
}

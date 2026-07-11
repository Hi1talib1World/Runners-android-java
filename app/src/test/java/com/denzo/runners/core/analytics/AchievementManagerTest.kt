package com.denzo.runners.core.analytics

import com.denzo.runners.data.local.entities.RunEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementManagerTest {

    private val manager = AchievementManager()

    @Test
    fun `first step achievement is unlocked after one run`() {
        val runs = listOf(
            RunEntity(id = 1, timestamp = 0, avgPace = 5.0, distanceMeters = 100.0, durationSeconds = 20, caloriesBurned = 10.0, pathPoints = emptyList())
        )
        val result = manager.calculateAchievements(runs)
        assertTrue(result.find { it.id == "first_run" }?.isUnlocked == true)
    }

    @Test
    fun `5k finisher achievement is unlocked when run distance is 5000m`() {
        val runs = listOf(
            RunEntity(id = 1, timestamp = 0, avgPace = 5.0, distanceMeters = 5000.1, durationSeconds = 1800, caloriesBurned = 400.0, pathPoints = emptyList())
        )
        val result = manager.calculateAchievements(runs)
        assertTrue(result.find { it.id == "5k_runner" }?.isUnlocked == true)
    }

    @Test
    fun `century club achievement is unlocked when total distance is 100km`() {
        val runs = listOf(
            RunEntity(id = 1, timestamp = 0, avgPace = 5.0, distanceMeters = 50000.0, durationSeconds = 18000, caloriesBurned = 4000.0, pathPoints = emptyList()),
            RunEntity(id = 2, timestamp = 0, avgPace = 5.0, distanceMeters = 50000.0, durationSeconds = 18000, caloriesBurned = 4000.0, pathPoints = emptyList())
        )
        val result = manager.calculateAchievements(runs)
        assertTrue(result.find { it.id == "century_club" }?.isUnlocked == true)
    }

    @Test
    fun `achievements are locked when criteria is not met`() {
        val runs = emptyList<RunEntity>()
        val result = manager.calculateAchievements(runs)
        result.forEach { 
            assertFalse("Achievement ${it.id} should be locked", it.isUnlocked)
        }
    }
}

package com.denzo.runners.core.analytics

import com.denzo.runners.data.local.entities.RunEntity
import javax.inject.Inject
import javax.inject.Singleton

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconResId: Int,
    val isUnlocked: Boolean = false
)

@Singleton
class AchievementManager @Inject constructor() {

    fun calculateAchievements(runs: List<RunEntity>): List<Achievement> {
        val achievements = mutableListOf<Achievement>()

        // Milestone: First Run
        achievements.add(Achievement(
            "first_run",
            "First Step",
            "Completed your first run!",
            android.R.drawable.ic_menu_directions,
            runs.isNotEmpty()
        ))

        // Milestone: 5K Runner
        val has5K = runs.any { it.distanceMeters >= 5000 }
        achievements.add(Achievement(
            "5k_runner",
            "5K Finisher",
            "Completed a 5,000m run.",
            android.R.drawable.ic_menu_compass,
            has5K
        ))

        // Milestone: Century Club (100km total)
        val totalDistance = runs.sumOf { it.distanceMeters }
        achievements.add(Achievement(
            "century_club",
            "Century Club",
            "Reached 100km total distance.",
            android.R.drawable.ic_menu_gallery,
            totalDistance >= 100000
        ))

        // Milestone: Early Bird (Run before 7 AM)
        // Note: This would need timestamp analysis, simplified for now
        achievements.add(Achievement(
            "early_bird",
            "Early Bird",
            "Finished a run before 7:00 AM.",
            android.R.drawable.ic_menu_day,
            false // Placeholder
        ))

        return achievements
    }
}

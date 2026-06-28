package com.denzo.runners.data.repository

import com.denzo.runners.data.local.dao.WorkoutDao
import com.denzo.runners.data.local.entities.StepType
import com.denzo.runners.data.local.entities.TrainingPlanEntity
import com.denzo.runners.data.local.entities.WorkoutEntity
import com.denzo.runners.data.local.entities.WorkoutStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val workoutDao: WorkoutDao
) {
    fun getAllPlans(): Flow<List<TrainingPlanEntity>> = workoutDao.getAllPlans()

    fun getActivePlan(): Flow<TrainingPlanEntity?> = workoutDao.getActivePlan()

    fun getWorkoutsForPlan(planId: Int): Flow<List<WorkoutEntity>> = workoutDao.getWorkoutsForPlan(planId)

    fun getNextWorkout(planId: Int): Flow<WorkoutEntity?> = workoutDao.getNextWorkout(planId)

    suspend fun enrollInPlan(planId: Int) {
        workoutDao.unenrollAll()
        workoutDao.enrollInPlan(planId)
    }

    suspend fun completeWorkout(workout: WorkoutEntity) {
        workoutDao.updateWorkout(workout.copy(isCompleted = true, completionTimestamp = System.currentTimeMillis()))
    }

    suspend fun seedMockPlans() {
        val existing = workoutDao.getAllPlans().first()
        if (existing.isNotEmpty()) return

        val plans = listOf(
            TrainingPlanEntity(name = "Couch to 5K", description = "Perfect for beginners starting their journey.", totalWeeks = 8, difficulty = "Beginner"),
            TrainingPlanEntity(name = "10K Speed Booster", description = "Intermediate plan focusing on pace and endurance.", totalWeeks = 6, difficulty = "Intermediate"),
            TrainingPlanEntity(name = "Marathon Master", description = "Advanced training for long-distance dominance.", totalWeeks = 12, difficulty = "Advanced")
        )

        plans.forEach { plan ->
            workoutDao.insertPlan(plan)
            // Seed first week of workouts for demo
            val workouts = listOf(
                WorkoutEntity(planId = 1, weekNumber = 1, dayNumber = 1, title = "First Step", steps = listOf(
                    WorkoutStep(StepType.WARMUP, 300, 0.0, "Walk briskly for 5 minutes"),
                    WorkoutStep(StepType.RUN, 60, 0.0, "Jog for 1 minute"),
                    WorkoutStep(StepType.WARMUP, 90, 0.0, "Walk for 90 seconds"),
                    WorkoutStep(StepType.COOLDOWN, 300, 0.0, "Easy walk to cool down")
                )),
                WorkoutEntity(planId = 1, weekNumber = 1, dayNumber = 3, title = "Consistency", steps = listOf(
                    WorkoutStep(StepType.WARMUP, 300, 0.0, "Walk briskly"),
                    WorkoutStep(StepType.RUN, 120, 0.0, "Steady run for 2 minutes"),
                    WorkoutStep(StepType.COOLDOWN, 300, 0.0, "Cool down")
                ))
            )
            workoutDao.insertWorkouts(workouts)
        }
    }
}

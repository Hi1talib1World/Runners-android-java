package com.denzo.runners.data.local.dao

import androidx.room.*
import com.denzo.runners.data.local.entities.TrainingPlanEntity
import com.denzo.runners.data.local.entities.WorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    // Training Plans
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: TrainingPlanEntity)

    @Query("SELECT * FROM training_plans")
    fun getAllPlans(): Flow<List<TrainingPlanEntity>>

    @Query("SELECT * FROM training_plans WHERE isEnrolled = 1 LIMIT 1")
    fun getActivePlan(): Flow<TrainingPlanEntity?>

    @Query("UPDATE training_plans SET isEnrolled = 0")
    suspend fun unenrollAll()

    @Query("UPDATE training_plans SET isEnrolled = 1 WHERE id = :planId")
    suspend fun enrollInPlan(planId: Int)

    // Workouts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkouts(workouts: List<WorkoutEntity>)

    @Query("SELECT * FROM workouts WHERE planId = :planId ORDER BY weekNumber, dayNumber")
    fun getWorkoutsForPlan(planId: Int): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE planId = :planId AND isCompleted = 0 LIMIT 1")
    fun getNextWorkout(planId: Int): Flow<WorkoutEntity?>

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)
}

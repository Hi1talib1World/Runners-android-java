package com.denzo.runners.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.denzo.runners.data.local.dao.ChallengeDao
import com.denzo.runners.data.local.dao.ConfigDao
import com.denzo.runners.data.local.dao.GearDao
import com.denzo.runners.data.local.dao.RouteDao
import com.denzo.runners.data.local.dao.RunDao
import com.denzo.runners.data.local.dao.WorkoutDao
import com.denzo.runners.data.local.entities.ChallengeEntity
import com.denzo.runners.data.local.entities.ConfigEntity
import com.denzo.runners.data.local.entities.GearEntity
import com.denzo.runners.data.local.entities.RouteEntity
import com.denzo.runners.data.local.entities.RunEntity
import com.denzo.runners.data.local.entities.RunTypeConverters
import com.denzo.runners.data.local.entities.TrainingPlanEntity
import com.denzo.runners.data.local.entities.WorkoutEntity
import com.denzo.runners.data.local.entities.WorkoutTypeConverters

@Database(entities = [RunEntity::class, ConfigEntity::class, RouteEntity::class, GearEntity::class, TrainingPlanEntity::class, WorkoutEntity::class, ChallengeEntity::class], version = 9, exportSchema = false)
@TypeConverters(RunTypeConverters::class, WorkoutTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getRunDao(): RunDao
    abstract fun getConfigDao(): ConfigDao
    abstract fun getRouteDao(): RouteDao
    abstract fun getGearDao(): GearDao
    abstract fun getWorkoutDao(): WorkoutDao
    abstract fun getChallengeDao(): ChallengeDao
}

package com.denzo.runners.core.di

import android.content.Context
import androidx.room.Room
import com.denzo.runners.core.database.AppDatabase
import com.denzo.runners.data.local.dao.ChallengeDao
import com.denzo.runners.data.local.dao.ConfigDao
import com.denzo.runners.data.local.dao.GearDao
import com.denzo.runners.data.local.dao.RouteDao
import com.denzo.runners.data.local.dao.RunDao
import com.denzo.runners.data.local.dao.WorkoutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "running_db.sqlite"
        ).fallbackToDestructiveMigration(true) // Handle schema changes during consolidation
        .build()
    }

    @Provides
    fun provideRunDao(database: AppDatabase): RunDao {
        return database.getRunDao()
    }

    @Provides
    fun provideConfigDao(database: AppDatabase): ConfigDao {
        return database.getConfigDao()
    }

    @Provides
    fun provideRouteDao(database: AppDatabase): RouteDao {
        return database.getRouteDao()
    }

    @Provides
    fun provideGearDao(database: AppDatabase): GearDao {
        return database.getGearDao()
    }

    @Provides
    fun provideWorkoutDao(database: AppDatabase): WorkoutDao {
        return database.getWorkoutDao()
    }

    @Provides
    fun provideChallengeDao(database: AppDatabase): ChallengeDao {
        return database.getChallengeDao()
    }
}

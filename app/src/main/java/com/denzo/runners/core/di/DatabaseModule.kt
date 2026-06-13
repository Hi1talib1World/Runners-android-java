package com.denzo.runners.core.di

import android.content.Context
import androidx.room.Room
import com.denzo.runners.core.database.AppDatabase
import com.denzo.runners.data.local.dao.ConfigDao
import com.denzo.runners.data.local.dao.RunDao
import com.denzo.runners.data.local.dao.RunningDAO
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
        ).build()
    }

    @Provides
    fun provideRunningDAO(database: AppDatabase): RunningDAO {
        return database.getRunningdataDAO()
    }

    @Provides
    fun provideRunDao(database: AppDatabase): RunDao {
        return database.getRunDao()
    }

    @Provides
    fun provideConfigDao(database: AppDatabase): ConfigDao {
        return database.getConfigDao()
    }
}

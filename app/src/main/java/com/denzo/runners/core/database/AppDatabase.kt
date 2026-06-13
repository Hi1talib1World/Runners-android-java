package com.denzo.runners.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.denzo.runners.data.local.dao.ConfigDao
import com.denzo.runners.data.local.dao.RunDao
import com.denzo.runners.data.local.dao.RunningDAO
import com.denzo.runners.data.local.entities.ConfigEntity
import com.denzo.runners.data.local.entities.RunEntity
import com.denzo.runners.data.local.entities.RunTypeConverters
import com.denzo.runners.data.local.entities.Runningdata

@Database(entities = [Runningdata::class, RunEntity::class, ConfigEntity::class], version = 4, exportSchema = false)
@TypeConverters(RunTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getRunningdataDAO(): RunningDAO
    abstract fun getRunDao(): RunDao
    abstract fun getConfigDao(): ConfigDao
}

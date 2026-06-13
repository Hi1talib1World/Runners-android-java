package com.denzo.runners.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_config")
data class ConfigEntity(
    @PrimaryKey val id: Int = 0,
    val isDarkMode: Boolean = true,
    val syncFrequencyMinutes: Int = 15,
    val lastSyncTimestamp: Long = 0L,
    val isTelemetryEnabled: Boolean = true
)

package com.denzo.runners.features.settings

import com.denzo.runners.data.local.dao.ConfigDao
import com.denzo.runners.data.local.entities.ConfigEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val configDao: ConfigDao
) {
    val settingsFlow: Flow<ConfigEntity> = configDao.getConfigFlow().map { 
        it ?: ConfigEntity() // Default if null
    }

    suspend fun updateTheme(isDarkMode: Boolean) {
        val current = configDao.getConfig() ?: ConfigEntity()
        configDao.saveConfig(current.copy(isDarkMode = isDarkMode))
    }

    suspend fun updateTelemetry(isEnabled: Boolean) {
        val current = configDao.getConfig() ?: ConfigEntity()
        configDao.saveConfig(current.copy(isTelemetryEnabled = isEnabled))
    }
}

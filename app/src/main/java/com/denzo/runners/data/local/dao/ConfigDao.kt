package com.denzo.runners.data.local.dao

import androidx.room.*
import com.denzo.runners.data.local.entities.ConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Query("SELECT * FROM app_config WHERE id = 0")
    fun getConfigFlow(): Flow<ConfigEntity?>

    @Query("SELECT * FROM app_config WHERE id = 0")
    suspend fun getConfig(): ConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: ConfigEntity)
}

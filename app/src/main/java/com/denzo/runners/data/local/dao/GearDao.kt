package com.denzo.runners.data.local.dao

import androidx.room.*
import com.denzo.runners.data.local.entities.GearEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GearDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGear(gear: GearEntity)

    @Update
    suspend fun updateGear(gear: GearEntity)

    @Delete
    suspend fun deleteGear(gear: GearEntity)

    @Query("SELECT * FROM gear_table ORDER BY purchaseTimestamp DESC")
    fun getAllGearFlow(): Flow<List<GearEntity>>

    @Query("SELECT * FROM gear_table WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveGear(): GearEntity?

    @Query("UPDATE gear_table SET isActive = 0")
    suspend fun deactivateAllGear()

    @Query("UPDATE gear_table SET isActive = 1 WHERE id = :gearId")
    suspend fun activateGear(gearId: Int)
}

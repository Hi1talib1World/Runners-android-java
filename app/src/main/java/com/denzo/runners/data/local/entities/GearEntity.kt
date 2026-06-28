package com.denzo.runners.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gear_table")
data class GearEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val brand: String,
    val model: String,
    val maxMileageMeters: Double,
    val currentMileageMeters: Double = 0.0,
    val isActive: Boolean = true,
    val isRetired: Boolean = false,
    val purchaseTimestamp: Long = System.currentTimeMillis()
)

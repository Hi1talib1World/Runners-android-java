package com.denzo.runners.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.osmdroid.util.GeoPoint

@Entity(tableName = "saved_routes")
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val distanceMeters: Double,
    val bestDurationSeconds: Long,
    val pathPoints: List<GeoPoint>,
    val timestamp: Long = System.currentTimeMillis()
)

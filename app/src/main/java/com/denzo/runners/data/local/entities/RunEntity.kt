package com.denzo.runners.data.local.entities

import androidx.room.*
import org.osmdroid.util.GeoPoint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val avgPace: Double,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val caloriesBurned: Double,
    val pathPoints: List<GeoPoint>
)

class RunTypeConverters {
    @TypeConverter
    fun fromPathPoints(value: List<GeoPoint>): String = Gson().toJson(value)

    @TypeConverter
    fun toPathPoints(value: String): List<GeoPoint> {
        val listType = object : TypeToken<List<GeoPoint>>() {}.type
        return Gson().fromJson(value, listType)
    }
}

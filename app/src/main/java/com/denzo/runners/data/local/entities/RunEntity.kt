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
    val pathPoints: List<GeoPoint>,
    val steps: Int = 0,
    val avgHeartRate: Int = 0,
    val heartRatePoints: List<Int> = emptyList(),
    val cadence: Int = 0,
    val cadencePoints: List<Int> = emptyList(),
    val zoneBreakdown: List<Long> = emptyList(), // Seconds in Zone 1-5
    val isSynced: Boolean = false
)

class RunTypeConverters {
    @TypeConverter
    fun fromPathPoints(value: List<GeoPoint>): String = Gson().toJson(value)

    @TypeConverter
    fun toPathPoints(value: String): List<GeoPoint> {
        val listType = object : TypeToken<List<GeoPoint>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromIntList(value: List<Int>): String = Gson().toJson(value)

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        val listType = object : TypeToken<List<Int>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromLongList(value: List<Long>): String = Gson().toJson(value)

    @TypeConverter
    fun toLongList(value: String): List<Long> {
        val listType = object : TypeToken<List<Long>>() {}.type
        return Gson().fromJson(value, listType)
    }
}

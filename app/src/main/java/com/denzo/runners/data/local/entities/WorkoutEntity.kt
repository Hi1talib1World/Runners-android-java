package com.denzo.runners.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val planId: Int,
    val weekNumber: Int,
    val dayNumber: Int,
    val title: String,
    val steps: List<WorkoutStep>,
    val isCompleted: Boolean = false,
    val completionTimestamp: Long = 0L
)

data class WorkoutStep(
    val type: StepType,
    val durationSeconds: Long = 0,
    val distanceMeters: Double = 0.0,
    val instruction: String
)

enum class StepType { WARMUP, RUN, SPRINT, COOLDOWN }

class WorkoutTypeConverters {
    @TypeConverter
    fun fromStepList(value: List<WorkoutStep>): String = Gson().toJson(value)

    @TypeConverter
    fun toStepList(value: String): List<WorkoutStep> {
        val listType = object : TypeToken<List<WorkoutStep>>() {}.type
        return Gson().fromJson(value, listType)
    }
}

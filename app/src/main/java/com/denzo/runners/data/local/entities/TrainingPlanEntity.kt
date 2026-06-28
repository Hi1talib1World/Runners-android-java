package com.denzo.runners.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_plans")
data class TrainingPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val totalWeeks: Int,
    val difficulty: String, // Beginner, Intermediate, Advanced
    val isEnrolled: Boolean = false,
    val enrollmentTimestamp: Long = 0L
)

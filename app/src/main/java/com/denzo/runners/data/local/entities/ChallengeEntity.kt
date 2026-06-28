package com.denzo.runners.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val targetDistanceMeters: Double,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val medalIconResId: Int,
    val isJoined: Boolean = false,
    val isCompleted: Boolean = false
)

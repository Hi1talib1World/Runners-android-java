package com.denzo.runners.core.health

import android.annotation.SuppressLint
import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.units.Distance
import com.denzo.runners.data.local.entities.RunEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    suspend fun hasAllPermissions(): Boolean {
        val permissions = setOf(
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(DistanceRecord::class),
            HealthPermission.getWritePermission(HeartRateRecord::class)
        )
        return try {
            healthConnectClient.permissionController.getGrantedPermissions().containsAll(permissions)
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("RestrictedApi")
    suspend fun writeRunToHealthConnect(run: RunEntity) {
        if (!hasAllPermissions()) return

        val startTime = Instant.ofEpochMilli(run.timestamp)
        val endTime = startTime.plusSeconds(run.durationSeconds)

        val sessionRecord = ExerciseSessionRecord(
            startTime = startTime,
            startZoneOffset = ZonedDateTime.now().offset,
            endTime = endTime,
            endZoneOffset = ZonedDateTime.now().offset,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            title = "Runners Activity"
        )

        val distanceRecord = DistanceRecord(
            startTime = startTime,
            startZoneOffset = ZonedDateTime.now().offset,
            endTime = endTime,
            endZoneOffset = ZonedDateTime.now().offset,
            distance = Distance.meters(run.distanceMeters)
        )

        try {
            healthConnectClient.insertRecords(listOf(sessionRecord, distanceRecord))
        } catch (e: Exception) {
            // Handle insertion failure
        }
    }
}

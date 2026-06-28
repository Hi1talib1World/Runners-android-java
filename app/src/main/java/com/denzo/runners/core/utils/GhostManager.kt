package com.denzo.runners.core.utils

import org.osmdroid.util.GeoPoint

object GhostManager {

    /**
     * Interpolates the ghost's position on a route based on current duration.
     * Assumes points are roughly equally spaced in time for this phase.
     * A more advanced version would use timestamps per point.
     */
    fun calculateGhostPosition(pathPoints: List<GeoPoint>, bestDuration: Long, currentDuration: Long): GeoPoint? {
        if (pathPoints.isEmpty()) return null
        if (currentDuration <= 0) return pathPoints.first()
        if (currentDuration >= bestDuration) return pathPoints.last()

        val progress = currentDuration.toDouble() / bestDuration.toDouble()
        val indexFloat = progress * (pathPoints.size - 1)
        val index = indexFloat.toInt()
        val fraction = indexFloat - index

        if (index >= pathPoints.size - 1) return pathPoints.last()

        val p1 = pathPoints[index]
        val p2 = pathPoints[index + 1]

        val lat = p1.latitude + (p2.latitude - p1.latitude) * fraction
        val lon = p1.longitude + (p2.longitude - p1.longitude) * fraction

        return GeoPoint(lat, lon)
    }

    fun calculateTimeDelta(distanceCovered: Double, routePoints: List<GeoPoint>, bestDuration: Long, currentDuration: Long): Long {
        // Find how long it took the 'best' run to reach the same distance
        // Simplified for now: compare progress of current run vs ghost at current time
        // return currentDuration - expectedDurationAtThisDistance
        return 0L // Placeholder for complex logic
    }
}

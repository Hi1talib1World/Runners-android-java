package com.denzo.runners.core.utils

import java.util.Locale

object UnitConverter {

    private const val METERS_IN_MILE = 1609.34

    /**
     * Formats distance in KM or MI.
     * Robustness: Added validation for negative values.
     */
    fun formatDistance(meters: Double, isMetric: Boolean): String {
        if (meters < 0) {
            return if (isMetric) "0.00 km" else "0.00 mi"
        }

        return if (isMetric) {
            String.format(Locale.getDefault(), "%.2f km", meters / 1000.0)
        } else {
            String.format(Locale.getDefault(), "%.2f mi", meters / METERS_IN_MILE)
        }
    }

    /**
     * Formats pace as min/km or min/mi.
     * Robustness: Added checks for NaN and Infinite values.
     */
    fun formatPace(paceMinPerKm: Double, isMetric: Boolean): String {
        if (paceMinPerKm <= 0 || paceMinPerKm.isNaN() || paceMinPerKm.isInfinite()) {
            return "0'00''"
        }
        
        val actualPace = if (isMetric) paceMinPerKm else paceMinPerKm * (METERS_IN_MILE / 1000.0)
        val minutes = actualPace.toInt()
        val seconds = ((actualPace - minutes) * 60).toInt()
        
        return if (isMetric) {
            String.format(Locale.getDefault(), "%d:%02d /km", minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%d:%02d /mi", minutes, seconds)
        }
    }

    /**
     * Formats duration.
     * Robustness: Handles negative duration inputs.
     */
    fun formatDuration(seconds: Long): String {
        if (seconds < 0) return "00:00"

        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format(Locale.getDefault(), "%d:%02d", minutes, secs)
        }
    }
}

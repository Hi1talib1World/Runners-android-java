package com.denzo.runners.core.utils

import java.util.Locale

object UnitConverter {

    private const val METERS_IN_MILE = 1609.34

    fun formatDistance(meters: Double, isMetric: Boolean): String {
        return if (isMetric) {
            String.format(Locale.getDefault(), "%.2f km", meters / 1000.0)
        } else {
            String.format(Locale.getDefault(), "%.2f mi", meters / METERS_IN_MILE)
        }
    }

    fun formatPace(paceMinPerKm: Double, isMetric: Boolean): String {
        if (paceMinPerKm <= 0) return "0'00''"
        
        return if (isMetric) {
            String.format(Locale.getDefault(), "%.2f /km", paceMinPerKm)
        } else {
            // Convert min/km to min/mile
            val paceMinPerMile = paceMinPerKm * (METERS_IN_MILE / 1000.0)
            String.format(Locale.getDefault(), "%.2f /mi", paceMinPerMile)
        }
    }
}

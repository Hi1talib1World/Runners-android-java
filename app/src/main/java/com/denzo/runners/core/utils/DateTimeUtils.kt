package com.denzo.runners.core.utils

import java.util.Locale

object DateTimeUtils {

    /**
     * Formats seconds into a HH:MM:SS or MM:SS string.
     */
    fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", m, s)
        }
    }
}

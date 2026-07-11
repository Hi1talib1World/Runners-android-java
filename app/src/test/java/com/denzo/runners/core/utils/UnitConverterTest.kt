package com.denzo.runners.core.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {

    @Test
    fun `formatDistance returns km for metric`() {
        val result = UnitConverter.formatDistance(5000.0, true)
        assertEquals("5.00 km", result)
    }

    @Test
    fun `formatDistance returns mi for imperial`() {
        // 1609.34 meters = 1 mile
        val result = UnitConverter.formatDistance(1609.34, false)
        assertEquals("1.00 mi", result)
    }

    @Test
    fun `formatPace returns min per km for metric`() {
        val result = UnitConverter.formatPace(5.5, true)
        assertEquals("5.50 /km", result)
    }

    @Test
    fun `formatPace returns min per mile for imperial`() {
        // 5.5 min/km -> 5.5 * 1.60934 min/mile = 8.85
        val result = UnitConverter.formatPace(5.5, false)
        assertEquals("8.85 /mi", result)
    }

    @Test
    fun `formatPace returns 0 for invalid input`() {
        assertEquals("0'00''", UnitConverter.formatPace(0.0, true))
        assertEquals("0'00''", UnitConverter.formatPace(-1.0, false))
    }
}

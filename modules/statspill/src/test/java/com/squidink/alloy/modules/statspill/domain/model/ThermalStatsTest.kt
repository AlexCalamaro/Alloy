package com.squidink.alloy.modules.statspill.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalStatsTest {

    @Test
    fun `ThermalStats has correct default id and name`() {
        val stats = ThermalStats()

        assertEquals("thermal", stats.id)
        assertEquals("Temperature", stats.name)
        assertEquals(StatCategory.THERMAL, stats.category)
    }

    @Test
    fun `ThermalStats implements StatType interface`() {
        val stats: StatType = ThermalStats(
            timestamp = 1234567890L,
            batteryTemperature = 35f,
            cpuTemperature = 40f,
            skinTemperature = 32f
        )

        assertNotNull(stats)
        assertEquals(StatCategory.THERMAL, stats.category)
    }

    @Test
    fun `ThermalStats stores temperature values correctly`() {
        val stats = ThermalStats(
            timestamp = 1234567890L,
            batteryTemperature = 35f,
            cpuTemperature = 40f,
            skinTemperature = 32f
        )

        assertEquals(35f, stats.batteryTemperature)
        assertEquals(40f, stats.cpuTemperature)
        assertEquals(32f, stats.skinTemperature)
    }

    @Test
    fun `maxTemperature returns highest temperature`() {
        val stats = ThermalStats(
            batteryTemperature = 35f,
            cpuTemperature = 40f,
            skinTemperature = 32f
        )

        assertEquals(40f, stats.maxTemperature!!, 0.01f)
    }

    @Test
    fun `maxTemperature returns null when all temperatures are null`() {
        val stats = ThermalStats()

        assertNull(stats.maxTemperature)
    }

    @Test
    fun `maxTemperature returns only non-null temperatures`() {
        val stats = ThermalStats(
            batteryTemperature = null,
            cpuTemperature = 40f,
            skinTemperature = null
        )

        assertEquals(40f, stats.maxTemperature!!, 0.01f)
    }

    @Test
    fun `averageTemperature calculates average correctly`() {
        val stats = ThermalStats(
            batteryTemperature = 30f,
            cpuTemperature = 40f,
            skinTemperature = 35f
        )

        assertEquals(35f, stats.averageTemperature!!, 0.01f)
    }

    @Test
    fun `averageTemperature returns null when all temperatures are null`() {
        val stats = ThermalStats()

        assertNull(stats.averageTemperature)
    }

    @Test
    fun `averageTemperature calculates with available temperatures only`() {
        val stats = ThermalStats(
            batteryTemperature = 30f,
            cpuTemperature = null,
            skinTemperature = 35f
        )

        assertEquals(32.5f, stats.averageTemperature!!, 0.01f)
    }

    @Test
    fun `ThermalStats default values are correct`() {
        val stats = ThermalStats()

        assertNull(stats.batteryTemperature)
        assertNull(stats.cpuTemperature)
        assertNull(stats.skinTemperature)
        assertNull(stats.maxTemperature)
        assertNull(stats.averageTemperature)
    }

    @Test
    fun `ThermalStats timestamp defaults to current time when not specified`() {
        val before = System.currentTimeMillis()
        val stats = ThermalStats()
        val after = System.currentTimeMillis()

        assertTrue(stats.timestamp >= before)
        assertTrue(stats.timestamp <= after)
    }
}

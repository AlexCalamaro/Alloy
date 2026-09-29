package com.squidink.alloy.modules.statspill.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryInfoTest {

    @Test
    fun `BatteryInfo has correct default id and name`() {
        val battery = BatteryInfo()

        assertEquals("battery", battery.id)
        assertEquals("Battery", battery.name)
        assertEquals(StatCategory.POWER, battery.category)
    }

    @Test
    fun `BatteryInfo implements StatType interface`() {
        val battery: StatType = BatteryInfo(
            timestamp = 1234567890L,
            level = 80,
            scale = 100,
            percentage = 80
        )

        assertNotNull(battery)
        assertEquals(StatCategory.POWER, battery.category)
    }

    @Test
    fun `BatteryInfo stores battery values correctly`() {
        val battery = BatteryInfo(
            timestamp = 1234567890L,
            level = 75,
            scale = 100,
            percentage = 75,
            health = 1,
            status = 2,
            temperature = 320, // 32.0 degrees Celsius
            voltage = 4200,
            isCharging = true
        )

        assertEquals(75, battery.level)
        assertEquals(100, battery.scale)
        assertEquals(75, battery.percentage)
        assertEquals(1, battery.health)
        assertEquals(2, battery.status)
        assertEquals(320, battery.temperature)
        assertEquals(4200, battery.voltage)
        assertEquals(true, battery.isCharging)
    }

    @Test
    fun `getTemperatureCelsius converts temperature correctly`() {
        val battery = BatteryInfo(temperature = 320) // 32.0 degrees Celsius

        assertEquals(32.0f, battery.getTemperatureCelsius(), 0.01f)
    }

    @Test
    fun `BatteryInfo default values are correct`() {
        val battery = BatteryInfo()

        assertEquals(0, battery.level)
        assertEquals(100, battery.scale)
        assertEquals(0, battery.percentage)
        assertEquals(0, battery.health)
        assertEquals(0, battery.status)
        assertEquals(0, battery.temperature)
        assertEquals(0, battery.voltage)
        assertEquals(false, battery.isCharging)
    }

    @Test
    fun `BatteryInfo timestamp defaults to current time when not specified`() {
        val before = System.currentTimeMillis()
        val battery = BatteryInfo()
        val after = System.currentTimeMillis()

        assertTrue(battery.timestamp >= before)
        assertTrue(battery.timestamp <= after)
    }
}

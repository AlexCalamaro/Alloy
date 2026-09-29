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

    @Test
    fun `getHealthString maps health codes to human-readable strings`() {
        assertEquals("Good", BatteryInfo(health = 2).getHealthString())
        assertEquals("Overheated", BatteryInfo(health = 3).getHealthString())
        assertEquals("Dead", BatteryInfo(health = 4).getHealthString())
        assertEquals("Over Voltage", BatteryInfo(health = 5).getHealthString())
        assertEquals("Failed", BatteryInfo(health = 6).getHealthString())
        assertEquals("Cold", BatteryInfo(health = 7).getHealthString())
        assertEquals("Unknown", BatteryInfo(health = 99).getHealthString())
    }

    @Test
    fun `getCurrentMilliAmperes converts microamperes correctly`() {
        val batteryCharging = BatteryInfo(currentMicroamperes = 1_850_000)
        assertEquals(1850, batteryCharging.getCurrentMilliAmperes())

        val batteryDraining = BatteryInfo(currentMicroamperes = -420_000)
        assertEquals(-420, batteryDraining.getCurrentMilliAmperes())

        val batteryNull = BatteryInfo(currentMicroamperes = null)
        assertEquals(null, batteryNull.getCurrentMilliAmperes())
    }

    @Test
    fun `PluggedSource display names are correct`() {
        assertEquals("AC Charger", PluggedSource.AC.getDisplayName())
        assertEquals("USB", PluggedSource.USB.getDisplayName())
        assertEquals("Wireless", PluggedSource.WIRELESS.getDisplayName())
        assertEquals("Not Plugged", PluggedSource.UNPLUGGED.getDisplayName())
    }
}

package com.squidink.alloy.modules.statspill.data.datasource

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryDataSourceTest {

    private val context: Context = mockk(relaxed = true)
    private val dataSource = BatteryDataSource(context)

    @Test
    fun `parseBatteryIntent calculates percentage and maps charging status`() {
        val intent = mockk<Intent>()
        every { intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) } returns 75
        every { intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100) } returns 100
        every { intent.getIntExtra(BatteryManager.EXTRA_HEALTH, any()) } returns BatteryManager.BATTERY_HEALTH_GOOD
        every { intent.getIntExtra(BatteryManager.EXTRA_STATUS, any()) } returns BatteryManager.BATTERY_STATUS_CHARGING
        every { intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) } returns 310
        every { intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) } returns 4200
        every { intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) } returns BatteryManager.BATTERY_PLUGGED_AC

        val batteryInfo = dataSource.parseBatteryIntent(intent)

        assertEquals(75, batteryInfo.percentage)
        assertEquals(75, batteryInfo.level)
        assertEquals(100, batteryInfo.scale)
        assertTrue(batteryInfo.isCharging)
        assertEquals(310, batteryInfo.temperature)
        assertEquals(31.0f, batteryInfo.getTemperatureCelsius())
        assertEquals(4200, batteryInfo.voltage)
        assertEquals(com.squidink.alloy.modules.statspill.domain.model.PluggedSource.AC, batteryInfo.pluggedSource)
        assertEquals("Good", batteryInfo.getHealthString())
    }

    @Test
    fun `parseBatteryIntent handles non-100 scale safely`() {
        val intent = mockk<Intent>()
        every { intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) } returns 5
        every { intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100) } returns 10
        every { intent.getIntExtra(BatteryManager.EXTRA_HEALTH, any()) } returns BatteryManager.BATTERY_HEALTH_OVERHEAT
        every { intent.getIntExtra(BatteryManager.EXTRA_STATUS, any()) } returns BatteryManager.BATTERY_STATUS_DISCHARGING
        every { intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) } returns 250
        every { intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) } returns 3800
        every { intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) } returns 0

        val batteryInfo = dataSource.parseBatteryIntent(intent)

        assertEquals(50, batteryInfo.percentage)
        assertEquals(false, batteryInfo.isCharging)
        assertEquals(com.squidink.alloy.modules.statspill.domain.model.PluggedSource.UNPLUGGED, batteryInfo.pluggedSource)
        assertEquals("Overheated", batteryInfo.getHealthString())
    }
}

package com.squidink.alloy.modules.statspill.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.squidink.alloy.modules.statspill.domain.model.ThermalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsColorUtilsTest {

    @Test
    fun `calculateStatusColor with low value and higherIsBetter=false returns green-dominant color`() {
        val color = calculateStatusColor(value = 10f, min = 0f, max = 100f, higherIsBetter = false)

        // Green should exceed Red
        assertTrue("Green should be greater than Red", color.green > color.red)
    }

    @Test
    fun `calculateStatusColor with high value and higherIsBetter=false returns red-dominant color`() {
        val color = calculateStatusColor(value = 90f, min = 0f, max = 100f, higherIsBetter = false)

        // Red should exceed Green
        assertTrue("Red should be greater than Green", color.red > color.green)
    }

    @Test
    fun `calculateStatusColor with 50 percent midpoint returns yellow color`() {
        val color = calculateStatusColor(value = 50f, min = 0f, max = 100f, higherIsBetter = false)

        // Yellow in RGB is formed by equal high Red and Green with lower Blue
        assertEquals(color.red, color.green, 0.02f)
        assertTrue("Red and Green should be greater than Blue", color.red > color.blue)
    }

    @Test
    fun `calculateStatusColor with higherIsBetter=true inverts direction for battery`() {
        val fullBattery = calculateStatusColor(value = 100f, min = 0f, max = 100f, higherIsBetter = true)
        val emptyBattery = calculateStatusColor(value = 0f, min = 0f, max = 100f, higherIsBetter = true)

        // 100% battery should be green-dominant
        assertTrue("Full battery should be green-dominant", fullBattery.green > fullBattery.red)

        // 0% battery should be red-dominant
        assertTrue("Empty battery should be red-dominant", emptyBattery.red > emptyBattery.green)
    }

    @Test
    fun `calculateStatusColor adjusts luminance for dark vs light theme`() {
        val lightColor = calculateStatusColor(value = 0f, isDarkTheme = false)
        val darkColor = calculateStatusColor(value = 0f, isDarkTheme = true)

        // Light theme should have higher luminance (soft pastel)
        assertTrue("Light theme should have higher luminance", lightColor.luminance() > darkColor.luminance())
        assertTrue("Light theme should have high luminance (>0.6)", lightColor.luminance() > 0.6f)
        assertTrue("Dark theme should have low luminance (<0.2)", darkColor.luminance() < 0.2f)
    }

    @Test
    fun `calculateStatusColor clamps out-of-bounds values`() {
        val belowMin = calculateStatusColor(value = -50f, min = 0f, max = 100f, higherIsBetter = false)
        val atMin = calculateStatusColor(value = 0f, min = 0f, max = 100f, higherIsBetter = false)
        assertEquals(atMin.red, belowMin.red, 0.001f)
        assertEquals(atMin.green, belowMin.green, 0.001f)

        val aboveMax = calculateStatusColor(value = 200f, min = 0f, max = 100f, higherIsBetter = false)
        val atMax = calculateStatusColor(value = 100f, min = 0f, max = 100f, higherIsBetter = false)
        assertEquals(atMax.red, aboveMax.red, 0.001f)
        assertEquals(atMax.green, aboveMax.green, 0.001f)
    }

    @Test
    fun `calculateStatusColor handles zero or invalid range safely`() {
        // Range is zero, should not throw or return NaN
        val color = calculateStatusColor(value = 50f, min = 100f, max = 100f)
        assertTrue(!color.red.isNaN())
        assertTrue(!color.green.isNaN())
        assertTrue(!color.blue.isNaN())
    }

    @Test
    fun `calculateStatusColor with zero saturation returns grayscale`() {
        val gray = calculateStatusColor(value = 50f, saturation = 0f)
        assertEquals(gray.red, gray.green, 0.01f)
        assertEquals(gray.green, gray.blue, 0.01f)
    }

    @Test
    fun `desaturate extension function removes saturation while preserving luminance`() {
        val vivid = Color(1f, 0f, 0f) // Pure red
        val desaturated = vivid.desaturate(0f) // Full desaturation

        assertEquals(desaturated.red, desaturated.green, 0.01f)
        assertEquals(desaturated.green, desaturated.blue, 0.01f)
        assertEquals(vivid.luminance(), desaturated.luminance(), 0.02f)
    }

    @Test
    fun `calculateValueColor works as backward-compatible helper`() {
        val lowVal = calculateValueColor(value = 10, maximum = 100, higherIsBetter = false)
        val highVal = calculateValueColor(value = 90, maximum = 100, higherIsBetter = false)

        assertTrue(lowVal.green > lowVal.red)
        assertTrue(highVal.red > highVal.green)
    }

    @Test
    fun `calculateThermalColor with direct cpu temp scales from green to red`() {
        val coolCpu = calculateThermalColor(cpuTemp = 30f, thermalStatus = ThermalStatus.NONE)
        val hotCpu = calculateThermalColor(cpuTemp = 60f, thermalStatus = ThermalStatus.NONE)

        assertTrue("Cool CPU should be green-dominant", coolCpu.green > coolCpu.red)
        assertTrue("Hot CPU should be red-dominant", hotCpu.red > hotCpu.green)
    }

    @Test
    fun `calculateThermalColor without cpu temp uses thermal status appropriately`() {
        val normalState = calculateThermalColor(cpuTemp = null, thermalStatus = ThermalStatus.NONE)
        val throttledState = calculateThermalColor(cpuTemp = null, thermalStatus = ThermalStatus.SEVERE)

        assertTrue("Normal thermal status should be green-dominant", normalState.green > normalState.red)
        assertTrue("Severe throttling status should be red-dominant", throttledState.red > throttledState.green)
    }

    @Test
    fun `calculateThermalColor without cpu temp and moderate status leans yellow`() {
        val lightThrottle = calculateThermalColor(cpuTemp = null, thermalStatus = ThermalStatus.LIGHT)
        // Light throttling should have close red and green components (yellow hue)
        assertEquals(lightThrottle.red, lightThrottle.green, 0.05f)
        assertTrue("Light throttle Red and Green should exceed Blue", lightThrottle.red > lightThrottle.blue)
    }
}

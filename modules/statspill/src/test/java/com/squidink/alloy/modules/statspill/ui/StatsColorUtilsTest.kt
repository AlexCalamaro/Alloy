package com.squidink.alloy.modules.statspill.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [StatsColorUtils] color calculation functions.
 */
class StatsColorUtilsTest {

    @Test
    fun `calculatePercentageColor returns green for low values`() {
        val color = calculatePercentageColor(0.1f)
        
        // Green should dominate at low values
        assertEquals(0.255f, color.red, 0.01f)
        assertEquals(0.9f, color.green, 0.01f)
        assertEquals(100f / 255f, color.blue, 0.01f)
    }

    @Test
    fun `calculatePercentageColor returns yellow for medium values`() {
        val color = calculatePercentageColor(0.5f)
        
        // At 50%, should be yellow (red + green)
        assertEquals(1.0f, color.red, 0.01f)
        assertEquals(0.5f, color.green, 0.01f)
        assertEquals(100f / 255f, color.blue, 0.01f)
    }

    @Test
    fun `calculatePercentageColor returns red for high values`() {
        val color = calculatePercentageColor(1.0f)
        
        // Red should dominate at high values
        assertEquals(1.0f, color.red, 0.01f)
        assertEquals(0f, color.green, 0.01f)
        assertEquals(100f / 255f, color.blue, 0.01f)
    }

    @Test
    fun `calculatePercentageColor clamps values above 1.0`() {
        val color = calculatePercentageColor(1.5f)
        
        // Should be same as 1.0f (clamped)
        assertEquals(1.0f, color.red, 0.01f)
        assertEquals(0f, color.green, 0.01f)
        assertEquals(100f / 255f, color.blue, 0.01f)
    }

    @Test
    fun `calculatePercentageColor handles zero values`() {
        val color = calculatePercentageColor(0f)
        
        // Should be green at zero
        assertEquals(0f, color.red, 0.01f)
        assertEquals(1.0f, color.green, 0.01f)
        assertEquals(100f / 255f, color.blue, 0.01f)
    }

    @Test
    fun `calculatePercentageColor with custom maximum`() {
        val color = calculatePercentageColor(50f, maximum = 100f)
        
        // 50/100 = 0.5, should be yellow-ish
        assertEquals(1.0f, color.red, 0.01f)
        assertEquals(0.5f, color.green, 0.01f)
        assertEquals(100f / 255f, color.blue, 0.01f)
    }

    @Test
    fun `calculateValueColor returns green for low values`() {
        val color = calculateValueColor(10, 100)
        
        // 10/100 = 0.1, green should dominate
        assertEquals(25f / 255f, color.red, 0.01f)
        assertEquals(230f / 255f, color.green, 0.01f)
        assertEquals(100f / 255f, color.blue, 0.01f)
    }

    @Test
    fun `calculateValueColor returns red for high values`() {
        val color = calculateValueColor(100, 100)
        
        // 100/100 = 1.0, red should dominate
        assertEquals(1.0f, color.red, 0.01f)
        assertEquals(0f, color.green, 0.01f)
        assertEquals(100f / 255f, color.blue, 0.01f)
    }

    @Test
    fun `calculateValueColor clamps negative values`() {
        val color = calculateValueColor(-10, 100)
        
        // Should be treated as 0 (green)
        assertEquals(0f, color.red, 0.01f)
        assertEquals(1.0f, color.green, 0.01f)
        assertEquals(100f / 255f, color.blue, 0.01f)
    }

    @Test
    fun `calculateValueColor handles zero maximum`() {
        val color = calculateValueColor(100, 0)
        
        // Should default to 0 ratio (green) when maximum is 0
        assertEquals(0f, color.red, 0.01f)
        assertEquals(1.0f, color.green, 0.01f)
        assertEquals(100f / 255f, color.blue, 0.01f)
    }

    @Test
    fun `calculateValueColor clamps values above maximum`() {
        val color = calculateValueColor(150, 100)
        
        // Should be treated as 1.0 (red) when value exceeds maximum
        assertEquals(1.0f, color.red, 0.01f)
        assertEquals(0f, color.green, 0.01f)
        assertEquals(100f / 255f, color.blue, 0.01f)
    }

    @Test
    fun `color gradient is smooth across range`() {
        val colors = (0..10).map { i ->
            calculatePercentageColor(i / 10f)
        }
        
        // Red should increase monotonically
        for (i in 1 until colors.size) {
            assertTrue("Red should increase: ${colors[i-1].red} < ${colors[i].red}",
                colors[i].red >= colors[i - 1].red)
        }
        
        // Green should decrease monotonically
        for (i in 1 until colors.size) {
            assertTrue("Green should decrease: ${colors[i-1].green} > ${colors[i].green}",
                colors[i].green <= colors[i - 1].green)
        }
    }
}

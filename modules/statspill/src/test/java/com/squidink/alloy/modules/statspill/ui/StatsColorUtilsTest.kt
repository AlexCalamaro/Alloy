package com.squidink.alloy.modules.statspill.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsColorUtilsTest {

    @Test
    fun `calculateValueColor with low value returns green-dominant color`() {
        val color = calculateValueColor(10, 100)

        // Low value should have low R and high G
        assertTrue(color.red < 0.5f)
        assertTrue(color.green > 0.5f)
    }

    @Test
    fun `calculateValueColor with high value returns red-dominant color`() {
        val color = calculateValueColor(90, 100)

        // High value should have high R and low G
        assertTrue(color.red > 0.5f)
        assertTrue(color.green < 0.5f)
    }

    @Test
    fun `calculateValueColor with zero value`() {
        val color = calculateValueColor(0, 100)

        assertEquals(0f, color.red, 0.01f)
        assertEquals(1f, color.green, 0.01f)
        assertEquals(100f / 255f, color.blue, 0.01f)
    }

    @Test
    fun `calculateValueColor with maximum value`() {
        val color = calculateValueColor(100, 100)

        assertEquals(1f, color.red, 0.01f)
        assertEquals(0f, color.green, 0.01f)
        assertEquals(100f / 255f, color.blue, 0.01f)
    }

    @Test
    fun `calculateValueColor handles zero maximum`() {
        val color = calculateValueColor(50, 0)

        // Should default to ratio 0 when maximum is 0
        assertEquals(0f, color.red, 0.01f)
        assertEquals(1f, color.green, 0.01f)
    }

    @Test
    fun `calculateValueColor clamps ratio to 0-1 range`() {
        val color = calculateValueColor(150, 100)

        // Value exceeds maximum, should be clamped
        assertEquals(1f, color.red, 0.01f)
        assertEquals(0f, color.green, 0.01f)
    }

    @Test
    fun `calculateValueColor handles negative value`() {
        val color = calculateValueColor(-10, 100)

        // Negative value should be clamped to 0
        assertEquals(0f, color.red, 0.01f)
        assertEquals(1f, color.green, 0.01f)
    }

    @Test
    fun `calculateValueColor blue component is always 100`() {
        val color1 = calculateValueColor(0, 100)
        val color2 = calculateValueColor(50, 100)
        val color3 = calculateValueColor(100, 100)

        assertEquals(100f / 255f, color1.blue, 0.01f)
        assertEquals(100f / 255f, color2.blue, 0.01f)
        assertEquals(100f / 255f, color3.blue, 0.01f)
    }
}

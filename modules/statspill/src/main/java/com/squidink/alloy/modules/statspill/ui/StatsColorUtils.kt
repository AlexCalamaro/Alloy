package com.squidink.alloy.modules.statspill.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.squidink.alloy.modules.statspill.domain.model.ThermalStatus

/**
 * Calculates a smooth, desaturated status color transitioning through
 * Green (optimal) -> Yellow (moderate) -> Red (critical).
 *
 * @param value The current metric value.
 * @param min The minimum expected value for this metric (defaults to 0f).
 * @param max The maximum expected value for this metric (defaults to 100f).
 * @param higherIsBetter If true, higher values map towards green (e.g. Battery: 100% is green, 0% is red).
 *                       If false, lower values map towards green (e.g. CPU: 0% is green, 100% is red).
 * @param isDarkTheme Adjusts lightness and saturation for dark vs light theme backgrounds.
 * @param saturation Saturation level in 0.0f..1.0f (defaults to 0.35f for calm, desaturated, non-harsh tones).
 * @return A smooth [Color] matching the status.
 */
fun calculateStatusColor(
    value: Float,
    min: Float = 0f,
    max: Float = 100f,
    higherIsBetter: Boolean = false,
    isDarkTheme: Boolean = false,
    saturation: Float = 0.35f
): Color {
    val range = max - min
    val ratio = if (range > 0f) {
        ((value - min) / range).coerceIn(0f, 1f)
    } else {
        0f
    }

    // Effective fraction: 1.0 is optimal (green, 120°), 0.0 is critical (red, 0°)
    val effectiveFraction = if (higherIsBetter) ratio else (1f - ratio)

    // Hue: 120° (Green) down to 0° (Red), passing smoothly through 60° (Yellow) at midpoint 0.5
    val hue = (120f * effectiveFraction).coerceIn(0f, 120f)

    // Lightness: In light theme, soft pastel (0.90f). In dark theme, deep muted container (0.22f).
    val lightness = if (isDarkTheme) 0.22f else 0.90f
    val clampedSaturation = saturation.coerceIn(0f, 1f)

    return Color.hsl(
        hue = hue,
        saturation = clampedSaturation,
        lightness = lightness
    )
}

/**
 * Calculates a desaturated status color for percentage values (0.0 to 100.0).
 *
 * @param percentage The usage percentage (0.0 to 100.0).
 * @param higherIsBetter If true, 100% is green and 0% is red. If false, 0% is green and 100% is red.
 * @param saturation Saturation level (0.0 to 1.0).
 */
@Composable
fun calculatePercentageColor(
    percentage: Float,
    higherIsBetter: Boolean = false,
    saturation: Float = 0.35f
): Color {
    val normalized = if (percentage in 0f..1f && percentage > 0f) percentage * 100f else percentage
    return calculateStatusColor(
        value = normalized,
        min = 0f,
        max = 100f,
        higherIsBetter = higherIsBetter,
        isDarkTheme = isSystemInDarkTheme(),
        saturation = saturation
    )
}

/**
 * Calculates a desaturated background color for battery based on level (0 to 100).
 * For batteries, higher percentage is better (100% = Green, 0% = Red).
 */
@Composable
fun calculateBatteryColor(
    percentage: Int,
    saturation: Float = 0.35f
): Color {
    return calculateStatusColor(
        value = percentage.toFloat(),
        min = 0f,
        max = 100f,
        higherIsBetter = true,
        isDarkTheme = isSystemInDarkTheme(),
        saturation = saturation
    )
}

/**
 * Calculates a desaturated background color for thermal status.
 * Prioritizes direct CPU temperature when available.
 * If CPU sensor is restricted by OS, evaluates PowerManager thermal throttling status
 * and fallback (battery) temperature.
 */
@Composable
fun calculateTemperatureColor(
    cpuTemp: Float?,
    thermalStatus: ThermalStatus,
    fallbackTemp: Float? = null,
    saturation: Float = 0.35f
): Color {
    val isDark = isSystemInDarkTheme()
    return calculateThermalColor(
        cpuTemp = cpuTemp,
        thermalStatus = thermalStatus,
        fallbackTemp = fallbackTemp,
        isDarkTheme = isDark,
        saturation = saturation
    )
}

/**
 * Pure function to calculate thermal color for JVM and UI tests.
 */
fun calculateThermalColor(
    cpuTemp: Float?,
    thermalStatus: ThermalStatus,
    fallbackTemp: Float? = null,
    isDarkTheme: Boolean = false,
    saturation: Float = 0.35f
): Color {
    if (cpuTemp != null) {
        return calculateStatusColor(
            value = cpuTemp,
            min = 30f,
            max = 60f,
            higherIsBetter = false,
            isDarkTheme = isDarkTheme,
            saturation = saturation
        )
    }

    val statusScore = when (thermalStatus) {
        ThermalStatus.NONE -> 0.0f
        ThermalStatus.LIGHT -> 0.45f
        ThermalStatus.MODERATE -> 0.70f
        ThermalStatus.SEVERE,
        ThermalStatus.CRITICAL,
        ThermalStatus.EMERGENCY,
        ThermalStatus.SHUTDOWN -> 1.0f
        ThermalStatus.UNKNOWN -> 0.0f
    }

    val effectiveScore = if (statusScore == 0.0f && fallbackTemp != null) {
        ((fallbackTemp - 30f) / 20f).coerceIn(0f, 1f) * 0.4f
    } else {
        statusScore
    }

    return calculateStatusColor(
        value = effectiveScore,
        min = 0f,
        max = 1f,
        higherIsBetter = false,
        isDarkTheme = isDarkTheme,
        saturation = saturation
    )
}

/**
 * Calculates a desaturated background color for temperature readings in Celsius.
 * Lower temperature is better (<=30°C = Green, >=55°C = Red).
 * Returns null if no sensor reading is available (neutral card).
 */
@Composable
fun calculateTemperatureColor(
    maxTemp: Float?,
    minTemp: Float = 30f,
    maxSafeTemp: Float = 55f,
    saturation: Float = 0.35f
): Color? {
    if (maxTemp == null) return null
    return calculateStatusColor(
        value = maxTemp,
        min = minTemp,
        max = maxSafeTemp,
        higherIsBetter = false,
        isDarkTheme = isSystemInDarkTheme(),
        saturation = saturation
    )
}

/**
 * Calculate a smooth status color based on an integer value and maximum.
 */
fun calculateValueColor(
    value: Int,
    maximum: Int,
    higherIsBetter: Boolean = false,
    isDarkTheme: Boolean = false,
    saturation: Float = 0.35f
): Color {
    return calculateStatusColor(
        value = value.toFloat(),
        min = 0f,
        max = maximum.toFloat(),
        higherIsBetter = higherIsBetter,
        isDarkTheme = isDarkTheme,
        saturation = saturation
    )
}

/**
 * Returns a desaturated copy of this color while preserving relative luminance.
 *
 * @param factor 0.0f means completely desaturated (grayscale), 1.0f means unchanged.
 */
fun Color.desaturate(factor: Float = 0.5f): Color {
    val clampedFactor = factor.coerceIn(0f, 1f)
    val lum = luminance()
    // Gamma-correct linear luminance back to sRGB component space:
    val gray = if (lum <= 0.0031308f) {
        12.92f * lum
    } else {
        1.055f * Math.pow(lum.toDouble(), 1.0 / 2.4).toFloat() - 0.055f
    }.coerceIn(0f, 1f)

    val r = gray + (red - gray) * clampedFactor
    val g = gray + (green - gray) * clampedFactor
    val b = gray + (blue - gray) * clampedFactor
    return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f), alpha)
}

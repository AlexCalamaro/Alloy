package com.squidink.alloy.modules.statspill.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.squidink.alloy.core.design.desktopHover

/**
 * Configuration for a stat card's appearance and content.
 */
data class StatCardConfig(
    val title: String,
    val primaryValue: String,
    val secondaryValues: List<StatValue>,
    val colorScheme: CardColorScheme = CardColorScheme.SURFACE_VARIANT,
    val showPercentage: Boolean = true
)

/**
 * A labeled value displayed in a stat card.
 */
data class StatValue(
    val label: String,
    val value: String,
    val style: TextStyle = TextStyle.Default
)

/**
 * Color scheme options for stat cards based on usage level.
 */
enum class CardColorScheme {
    SURFACE_VARIANT,   // Default neutral color
    PRIMARY_CONTAINER,  // Low usage / good status
    WARNING,           // Medium usage / warning status
    ERROR,             // High usage / critical status
    CUSTOM             // For special cases with custom colors
}

/**
 * Calculates the background color based on usage percentage.
 * Returns green → yellow → red gradient.
 */
@Composable
fun calculatePercentageColor(percentage: Float): Color {
    return when {
        percentage < 50 -> MaterialTheme.colorScheme.primaryContainer
        percentage < 80 -> Color(0xFFFFA726) // Orange/Yellow
        else -> MaterialTheme.colorScheme.error
    }
}

/**
 * Calculates the background color for battery based on level.
 */
@Composable
fun calculateBatteryColor(percentage: Int): Color {
    return when {
        percentage < 20 -> MaterialTheme.colorScheme.error
        percentage < 50 -> Color(0xFFFFA726) // Orange
        else -> MaterialTheme.colorScheme.primaryContainer
    }
}

/**
 * Calculates the background color for temperature.
 */
@Composable
fun calculateTemperatureColor(maxTemp: Float?): Color {
    return when {
        maxTemp == null -> MaterialTheme.colorScheme.surfaceVariant
        maxTemp > 45f -> MaterialTheme.colorScheme.error
        maxTemp > 35f -> Color(0xFFFFA726) // Orange
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}

/**
 * Reusable stat card composable that displays resource usage information.
 *
 * @param config The configuration containing all display data
 * @param modifier Modifier to apply to the card
 */
@Composable
fun StatCard(
    config: StatCardConfig,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (config.colorScheme) {
        CardColorScheme.SURFACE_VARIANT -> MaterialTheme.colorScheme.surfaceVariant
        CardColorScheme.PRIMARY_CONTAINER -> MaterialTheme.colorScheme.primaryContainer
        CardColorScheme.WARNING -> Color(0xFFFFA726)
        CardColorScheme.ERROR -> MaterialTheme.colorScheme.error
        CardColorScheme.CUSTOM -> MaterialTheme.colorScheme.surfaceVariant
    }

    val onTextColor = when (config.colorScheme) {
        CardColorScheme.SURFACE_VARIANT -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onPrimary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .desktopHover(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = config.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = onTextColor,
                    modifier = Modifier.weight(1f)
                )
                
                config.primaryValue.takeIf { it.isNotEmpty() }?.let { value ->
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = onTextColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            config.secondaryValues.forEach { statValue ->
                Text(
                    text = "${statValue.label}: ${statValue.value}",
                    style = statValue.style,
                    color = onTextColor
                )
            }
        }
    }
}

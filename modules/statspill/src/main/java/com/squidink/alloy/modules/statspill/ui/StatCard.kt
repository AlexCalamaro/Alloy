package com.squidink.alloy.modules.statspill.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
    val customBackgroundColor: Color? = null,
    val customContentColor: Color? = null,
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
    PRIMARY_CONTAINER,  // Low usage / optimal status
    WARNING,           // Moderate-to-high usage / warning status
    ERROR,             // High usage / critical status
    CUSTOM             // For special cases
}

/**
 * Reusable stat card composable following Material 3 guidelines and WCAG AA contrast.
 */
@Composable
fun StatCard(
    config: StatCardConfig,
    modifier: Modifier = Modifier
) {
    val backgroundColor = config.customBackgroundColor ?: when (config.colorScheme) {
        CardColorScheme.SURFACE_VARIANT -> MaterialTheme.colorScheme.surfaceVariant
        CardColorScheme.PRIMARY_CONTAINER -> MaterialTheme.colorScheme.primaryContainer
        CardColorScheme.WARNING -> Color(0xFFFFD54F) // Accessible amber container
        CardColorScheme.ERROR -> MaterialTheme.colorScheme.errorContainer
        CardColorScheme.CUSTOM -> MaterialTheme.colorScheme.surfaceVariant
    }

    val onTextColor = config.customContentColor ?: if (config.customBackgroundColor != null) {
        // High-contrast text calculated from container luminance for accessibility
        if (config.customBackgroundColor.luminance() > 0.45f) {
            Color(0xFF1C1B1F)
        } else {
            Color(0xFFF4EFF4)
        }
    } else {
        when (config.colorScheme) {
            CardColorScheme.SURFACE_VARIANT -> MaterialTheme.colorScheme.onSurfaceVariant
            CardColorScheme.PRIMARY_CONTAINER -> MaterialTheme.colorScheme.onPrimaryContainer
            CardColorScheme.WARNING -> Color(0xFF261900) // High-contrast dark text on amber
            CardColorScheme.ERROR -> MaterialTheme.colorScheme.onErrorContainer
            CardColorScheme.CUSTOM -> MaterialTheme.colorScheme.onSurface
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .desktopHover(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = onTextColor
        ),
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

            if (config.secondaryValues.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                config.secondaryValues.forEach { statValue ->
                    Text(
                        text = if (statValue.label.isNotEmpty()) "${statValue.label}: ${statValue.value}" else statValue.value,
                        style = statValue.style,
                        color = onTextColor
                    )
                }
            }
        }
    }
}

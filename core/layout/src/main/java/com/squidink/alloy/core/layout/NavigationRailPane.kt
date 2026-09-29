package com.squidink.alloy.core.layout

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.squidink.alloy.core.navigation.Screens
import com.squidink.alloy.core.navigation.getNavigationIcon
import com.squidink.alloy.core.navigation.getScreenTitle

/**
 * Navigation rail component for expanded screen layouts.
 * Displays vertical navigation items with icons.
 *
 * @param currentFeature The currently selected feature route
 * @param screens List of screens to display in the rail
 * @param onScreenSelected Callback when a screen is selected
 * @param modifier Modifier to apply to the root container
 * @param header Optional header composable displayed at the top of the rail
 */
@Composable
fun NavigationRailPane(
    currentFeature: String,
    screens: List<Screens>,
    onScreenSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable ColumnScope.() -> Unit)? = null
) {
    NavigationRail(
        modifier = modifier,
        header = header ?: {
            Spacer(modifier = Modifier.height(20.dp))
        }
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        screens.forEachIndexed { index, screen ->
            val isSelected = currentFeature == screen.route
            
            NavigationRailItem(
                selected = isSelected,
                onClick = { onScreenSelected(screen.route) },
                icon = {
                    Icon(
                        imageVector = screen.getNavigationIcon(),
                        contentDescription = screen.route.getScreenTitle()
                    )
                },
                label = {
                    Text(text = screen.route.getScreenTitle())
                }
            )

            if (index < screens.lastIndex) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

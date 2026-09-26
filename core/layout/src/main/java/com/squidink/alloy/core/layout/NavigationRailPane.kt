package com.squidink.alloy.core.layout

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
 */
@Composable
fun NavigationRailPane(
    currentFeature: String,
    screens: List<Screens>,
    onScreenSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier
    ) {
        screens.forEach { screen ->
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
        }
    }
}

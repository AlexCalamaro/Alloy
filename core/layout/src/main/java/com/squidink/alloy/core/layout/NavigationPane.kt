package com.squidink.alloy.core.layout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.squidink.alloy.core.design.desktopHover
import com.squidink.alloy.core.navigation.Screens
import com.squidink.alloy.core.navigation.getScreenTitle

/**
 * Navigation pane component for the three-pane layout.
 * Displays feature navigation items with selection state.
 *
 * @param layoutController The layout state controller
 * @param screens List of screens to display in navigation
 * @param onScreenSelected Callback when a screen is selected
 * @param modifier Modifier to apply to the root container
 */
@Composable
fun NavigationPane(
    layoutController: LayoutController,
    screens: List<Screens>,
    onScreenSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val layoutState by layoutController.layoutState.collectAsState()
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Alloy Suite",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            
            // Close button for compact screens
            IconButton(onClick = { layoutController.closeNavigation() }) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Close navigation"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Navigation items
        LazyColumn {
            items(screens) { screen ->
                val isSelected = layoutState.currentFeature == screen.route
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable {
                            onScreenSelected(screen.route)
                            layoutController.selectFeature(screen.route)
                        }
                        .desktopHover(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Text(
                        text = screen.route.getScreenTitle(),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Footer info
        Text(
            text = "v1.0.0",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Compact navigation drawer with close button.
 * Used for overlay navigation on compact screens.
 */
@Composable
fun NavigationDrawer(
    layoutController: LayoutController,
    screens: List<Screens>,
    onScreenSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationPane(
        layoutController = layoutController,
        screens = screens,
        onScreenSelected = onScreenSelected,
        modifier = modifier
    )
}

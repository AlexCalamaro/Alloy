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
import com.squidink.alloy.core.feature.featureRegistry
import com.squidink.alloy.core.navigation.Screens
import com.squidink.alloy.core.navigation.getFeatureId
import com.squidink.alloy.core.navigation.getNavigationIcon
import com.squidink.alloy.core.navigation.getScreenTitle

/**
 * Navigation pane component for the modal drawer.
 * Displays feature navigation items with selection state.
 *
 * @param currentFeature The currently selected feature route
 * @param screens List of screens to display in navigation
 * @param onScreenSelected Callback when a screen is selected
 * @param onDismiss Optional callback to close the drawer (for compact screens)
 * @param modifier Modifier to apply to the root container
 */
@Composable
fun NavigationPane(
    currentFeature: String,
    screens: List<Screens>,
    onScreenSelected: (String) -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
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
            
            // Close button (optional - for compact screens)
            onDismiss?.let { dismiss ->
                IconButton(onClick = dismiss) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Close navigation"
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Navigation items
        LazyColumn {
            items(screens) { screen ->
                val isSelected = currentFeature == screen.route
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable {
                            onScreenSelected(screen.route)
                            onDismiss?.invoke()
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
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon from extension function
                        Icon(
                            imageVector = screen.getNavigationIcon(),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        
                        Text(
                            text = screen.route.getScreenTitle(),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
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

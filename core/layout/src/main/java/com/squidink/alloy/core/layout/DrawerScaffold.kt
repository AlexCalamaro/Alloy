package com.squidink.alloy.core.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.squidink.alloy.core.navigation.Screens
import kotlinx.coroutines.launch

/**
 * Simple scaffold with modal navigation drawer and top app bar.
 * 
 * Features:
 * - Navigation rail for expanded screens (> 840dp)
 * - Modal navigation drawer for compact/medium screens
 * - Top app bar with hamburger menu (only shown on compact/medium)
 * - Single content area - features manage their own detail panes
 * 
 * @param currentFeature The currently selected feature route
 * @param screens List of navigation screens
 * @param onScreenSelected Callback when a screen is selected
 * @param title The title to display in the top app bar
 * @param content The main content area
 * @param modifier Modifier to apply to the root container
 * @param railHeader Optional header composable to display at the top of the navigation rail
 */
@Composable
fun DrawerScaffold(
    currentFeature: String,
    screens: List<Screens>,
    onScreenSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    railHeader: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val windowSizeClass = deriveWindowSizeClass()
    
    if (windowSizeClass.isExpanded()) {
        // Navigation rail layout for expanded screens - no redundant top bar
        Row(
            modifier = modifier.fillMaxSize()
        ) {
            NavigationRailPane(
                currentFeature = currentFeature,
                screens = screens,
                onScreenSelected = onScreenSelected,
                header = railHeader
            )
            
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                content()
            }
        }
    } else {
        // Modal drawer layout for compact/medium screens - full screen content with gesture support
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                ModalDrawerSheet {
                    NavigationPane(
                        currentFeature = currentFeature,
                        screens = screens,
                        onScreenSelected = { route ->
                            onScreenSelected(route)
                            scope.launch { drawerState.close() }
                        },
                        onDismiss = { scope.launch { drawerState.close() } }
                    )
                }
            }
        ) {
            Surface(
                modifier = modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                content()
            }
        }
    }
}

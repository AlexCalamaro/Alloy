package com.squidink.alloy.core.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerScaffold(
    currentFeature: String,
    screens: List<Screens>,
    onScreenSelected: (String) -> Unit,
    title: String,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val windowSizeClass = deriveWindowSizeClass()
    
    if (windowSizeClass.isExpanded()) {
        // Navigation rail layout for expanded screens
        Row(
            modifier = modifier.fillMaxSize()
        ) {
            NavigationRailPane(
                currentFeature = currentFeature,
                screens = screens,
                onScreenSelected = onScreenSelected
            )
            
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(title) }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    content()
                }
            }
        }
    } else {
        // Modal drawer layout for compact/medium screens
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    NavigationPane(
                        currentFeature = currentFeature,
                        screens = screens,
                        onScreenSelected = onScreenSelected
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(title) },
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open navigation"
                                )
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    content()
                }
            }
        }
    }
}

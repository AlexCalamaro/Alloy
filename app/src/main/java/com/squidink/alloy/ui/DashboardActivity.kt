package com.squidink.alloy.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.squidink.alloy.core.datastore.DataStoreManager
import com.squidink.alloy.core.design.AlloyTheme
import com.squidink.alloy.core.design.desktopHover
import com.squidink.alloy.core.navigation.AlloyNavGraph
import com.squidink.alloy.core.navigation.Screens
import com.squidink.alloy.core.navigation.getScreenTitle
import com.squidink.alloy.modules.clip.ClipViewModel
import com.squidink.alloy.modules.clip.ui.ClipScreen
import com.squidink.alloy.modules.scenes.ScenesViewModel
import com.squidink.alloy.modules.scenes.ui.ScenesScreen
import com.squidink.alloy.modules.scratch.ScratchViewModel
import com.squidink.alloy.modules.scratch.ui.ScratchScreen
import com.squidink.alloy.modules.statspill.StatsViewModel
import com.squidink.alloy.modules.statspill.ui.StatsScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Main dashboard activity that hosts the navigation graph.
 *
 * Uses Navigation Compose for type-safe navigation between modules.
 * Supports Material You (dynamic color) based on user preference.
 */
@AndroidEntryPoint
class DashboardActivity : ComponentActivity() {
    @Inject
    lateinit var dataStoreManager: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Read dynamic color preference (defaults to true)
        val useDynamicColor = runBlocking {
            dataStoreManager.getDynamicColor().first()
        }

        setContent {
            DashboardScreen(dynamicColorEnabled = useDynamicColor)
        }
    }
}

/**
 * Main dashboard screen with navigation rail and content area.
 *
 * Uses Navigation Compose for screen navigation.
 * Supports Material You (dynamic color) theming.
 */
@Composable
fun DashboardScreen(dynamicColorEnabled: Boolean = true) {
    val navController: NavHostController = rememberNavController()

    AlloyTheme(dynamicColor = dynamicColorEnabled) {
        Scaffold { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Navigation Rail
                Card(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Alloy Suite",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        NavigationRail(navController = navController)
                    }
                }

                // Main Content Area with Navigation Graph
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    AlloyNavGraph(
                        navController = navController,
                        onStatsPill = {
                            val viewModel: StatsViewModel = hiltViewModel()
                            StatsScreen(viewModel = viewModel)
                        },
                        onClip = {
                            val viewModel: ClipViewModel = hiltViewModel()
                            ClipScreen(viewModel = viewModel)
                        },
                        onScratch = {
                            val viewModel: ScratchViewModel = hiltViewModel()
                            ScratchScreen(viewModel = viewModel)
                        },
                        onScenes = {
                            val viewModel: ScenesViewModel = hiltViewModel()
                            ScenesScreen(viewModel = viewModel)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Navigation rail with module tabs.
 *
 * @param navController The navigation controller for screen navigation
 */
@Composable
fun NavigationRail(navController: NavHostController) {
    val currentRoute by navController.currentBackStackEntryFlow
        .collectAsState(initial = navController.currentBackStackEntry)
    
    val route = currentRoute?.destination?.route ?: Screens.StatsPill.route

    LazyColumn {
        items(listOf(Screens.StatsPill, Screens.Clip, Screens.Scratch, Screens.Scenes)) { screen ->
            val isSelected = route == screen.route
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable { navController.navigate(screen.route) }
                    .desktopHover(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
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
}

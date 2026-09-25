package com.squidink.alloy.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.squidink.alloy.core.datastore.DataStoreManager
import com.squidink.alloy.core.design.AlloyTheme
import com.squidink.alloy.core.layout.DrawerScaffold
import com.squidink.alloy.core.layout.NavigationPane
import com.squidink.alloy.core.navigation.Screens
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
 * Main dashboard screen with modal navigation drawer.
 *
 * Uses DrawerScaffold for a simple navigation drawer + content layout.
 * Supports Material You (dynamic color) theming.
 * Integrates feature-specific screens via navigation.
 * Includes TopAppBar with hamburger menu (always visible).
 * Supports keyboard shortcuts for navigation.
 */
@Composable
fun DashboardScreen(dynamicColorEnabled: Boolean = true) {
    val navController: NavHostController = rememberNavController()

    AlloyTheme(dynamicColor = dynamicColorEnabled) {
        Box(
            modifier = Modifier
        ) {
            DrawerScaffold(
                title = "Alloy Suite",
                navigationContent = {
                    NavigationPane(
                        currentFeature = navController.currentDestination?.route ?: Screens.StatsPill.route,
                        screens = listOf(Screens.StatsPill, Screens.Clip, Screens.Scratch, Screens.Scenes),
                        onScreenSelected = { route ->
                            navController.navigate(route)
                        }
                    )
                },
                content = {
                    NavHost(
                        navController = navController,
                        startDestination = Screens.StatsPill.route
                    ) {
                        composable(Screens.StatsPill.route) {
                            val viewModel: StatsViewModel = hiltViewModel()
                            StatsScreen(viewModel = viewModel)
                        }
                        composable(Screens.Clip.route) {
                            val viewModel: ClipViewModel = hiltViewModel()
                            ClipScreen(viewModel = viewModel)
                        }
                        composable(Screens.Scratch.route) {
                            val viewModel: ScratchViewModel = hiltViewModel()
                            ScratchScreen(viewModel = viewModel)
                        }
                        composable(Screens.Scenes.route) {
                            val viewModel: ScenesViewModel = hiltViewModel()
                            ScenesScreen(viewModel = viewModel)
                        }
                    }
                }
            )
        }
    }
}

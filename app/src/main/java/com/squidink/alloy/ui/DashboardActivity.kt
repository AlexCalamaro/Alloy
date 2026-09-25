package com.squidink.alloy.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.squidink.alloy.core.datastore.DataStoreManager
import com.squidink.alloy.core.design.AlloyTheme
import com.squidink.alloy.core.layout.DetailPane
import com.squidink.alloy.core.layout.FeatureDetail
import com.squidink.alloy.core.layout.LayoutController
import com.squidink.alloy.core.layout.NavigationPane
import com.squidink.alloy.core.layout.ThreePaneScaffold
import com.squidink.alloy.core.layout.WindowSizeClass
import com.squidink.alloy.core.layout.deriveWindowSizeClass
import com.squidink.alloy.core.navigation.AlloyNavGraph
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
 * Main dashboard screen with three-pane responsive layout.
 *
 * Uses ThreePaneScaffold for adaptive layout across screen sizes.
 * Supports Material You (dynamic color) theming.
 */
@Composable
fun DashboardScreen(dynamicColorEnabled: Boolean = true) {
    val navController: NavHostController = rememberNavController()
    val layoutController: LayoutController = hiltViewModel()
    val windowSizeClass: WindowSizeClass = deriveWindowSizeClass()
    val layoutState by layoutController.layoutState.collectAsState()

    AlloyTheme(dynamicColor = dynamicColorEnabled) {
        ThreePaneScaffold(
            windowSizeClass = windowSizeClass,
            layoutController = layoutController,
            navigationContent = {
                NavigationPane(
                    layoutController = layoutController,
                    screens = listOf(Screens.StatsPill, Screens.Clip, Screens.Scratch, Screens.Scenes),
                    onScreenSelected = { route ->
                        navController.navigate(route)
                    }
                )
            },
            contentContent = {
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
            },
            detailContent = {
                // TODO: Feature-specific detail content will be provided by features
                // For now, show empty detail pane
                DetailPane(
                    layoutController = layoutController,
                    title = "Settings",
                    detailContent = {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Placeholder for feature settings
                        }
                    }
                )
            }
        )
    }
}

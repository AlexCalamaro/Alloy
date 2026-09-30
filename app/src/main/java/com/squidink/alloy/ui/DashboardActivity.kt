package com.squidink.alloy.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.squidink.alloy.core.datastore.DataStoreManager
import com.squidink.alloy.core.design.AlloyTheme
import com.squidink.alloy.core.feature.featureRegistry
import com.squidink.alloy.core.layout.DrawerScaffold
import com.squidink.alloy.core.navigation.Screens
import com.squidink.alloy.core.navigation.getFeatureId
import com.squidink.alloy.modules.lists.ListsViewModel
import com.squidink.alloy.modules.lists.ui.ListsScreen
import com.squidink.alloy.modules.rssreader.RssReaderViewModel
import com.squidink.alloy.modules.rssreader.ui.RssReaderScreen
import com.squidink.alloy.modules.scenes.ScenesViewModel
import com.squidink.alloy.modules.scenes.ui.ScenesScreen
import com.squidink.alloy.modules.scratch.ScratchViewModel
import com.squidink.alloy.modules.scratch.ui.ScratchScreen
import com.squidink.alloy.modules.settings.SettingsViewModel
import com.squidink.alloy.modules.settings.ui.SettingsScreen
import com.squidink.alloy.modules.statspill.StatsViewModel
import com.squidink.alloy.modules.statspill.ui.StatsScreen
import dagger.hilt.android.AndroidEntryPoint
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

    private val targetScreenState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        targetScreenState.value = intent?.getStringExtra(EXTRA_TARGET_SCREEN)
        
        setContent {
            val targetScreen by targetScreenState
            val useDynamicColor by dataStoreManager.getDynamicColor().collectAsState(initial = true)
            DashboardScreen(
                dynamicColorEnabled = useDynamicColor,
                targetScreen = targetScreen
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_TARGET_SCREEN)?.let { target ->
            targetScreenState.value = target
        }
    }

    companion object {
        const val EXTRA_TARGET_SCREEN = "target_screen"
    }
}

/**
 * Main dashboard screen with navigation rail/drawer.
 *
 * Uses DrawerScaffold for responsive navigation layout.
 * Supports Material You (dynamic color) theming.
 * Integrates feature-specific screens via navigation.
 * - Navigation rail for expanded screens (> 840dp)
 * - Modal drawer for compact/medium screens
 */
@Composable
fun DashboardScreen(
    dynamicColorEnabled: Boolean = true,
    targetScreen: String? = null
) {
    val navController: NavHostController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screens.StatsPill.route
    val featureRegistryInstance = featureRegistry()
    val featureRegistry by featureRegistryInstance.features.collectAsState()
    val featureStates by featureRegistryInstance.featureStates.collectAsState()

    LaunchedEffect(targetScreen) {
        if (!targetScreen.isNullOrBlank() && targetScreen != currentRoute) {
            navController.navigate(targetScreen) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    
    val allScreens = listOf(
        Screens.StatsPill,
        Screens.RssReader,
        Screens.Scratch,
        Screens.Lists,
        Screens.Scenes,
        Screens.Settings
    )

    // Filter screens dynamically based on feature enabled states
    val visibleScreens = allScreens.filter { screen ->
        val featureId = screen.getFeatureId()
        if (featureId.isEmpty() || featureId == "settings") true
        else featureStates[featureId] != false
    }

    AlloyTheme(dynamicColor = dynamicColorEnabled) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            DrawerScaffold(
                currentFeature = currentRoute,
                screens = visibleScreens,
                onScreenSelected = { route ->
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
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
                        composable(Screens.RssReader.route) {
                            val viewModel: RssReaderViewModel = hiltViewModel()
                            RssReaderScreen(viewModel = viewModel)
                        }
                        composable(Screens.Scratch.route) {
                            val viewModel: ScratchViewModel = hiltViewModel()
                            ScratchScreen(viewModel = viewModel)
                        }
                        composable(Screens.Lists.route) {
                            val viewModel: ListsViewModel = hiltViewModel()
                            ListsScreen(viewModel = viewModel)
                        }
                        composable(Screens.Scenes.route) {
                            val viewModel: ScenesViewModel = hiltViewModel()
                            ScenesScreen(viewModel = viewModel)
                        }
                        composable(Screens.Settings.route) {
                            val viewModel: SettingsViewModel = hiltViewModel()
                            SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            )
        }
    }
}

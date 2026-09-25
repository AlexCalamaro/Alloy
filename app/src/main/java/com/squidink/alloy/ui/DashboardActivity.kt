package com.squidink.alloy.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.squidink.alloy.core.navigation.getScreenTitle
import com.squidink.alloy.modules.clip.ClipFeatureDetail
import com.squidink.alloy.modules.clip.ClipViewModel
import com.squidink.alloy.modules.clip.ui.ClipScreen
import com.squidink.alloy.modules.scenes.ScenesFeatureDetail
import com.squidink.alloy.modules.scenes.ScenesViewModel
import com.squidink.alloy.modules.scenes.ui.ScenesScreen
import com.squidink.alloy.modules.scratch.ScratchFeatureDetail
import com.squidink.alloy.modules.scratch.ScratchViewModel
import com.squidink.alloy.modules.scratch.ui.ScratchScreen
import com.squidink.alloy.modules.statspill.StatsPillFeatureDetail
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
 * Integrates feature-specific detail content based on current feature.
 * Includes TopAppBar with hamburger menu for compact screens.
 * Supports keyboard shortcuts for navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(dynamicColorEnabled: Boolean = true) {
    val navController: NavHostController = rememberNavController()
    val layoutController: LayoutController = hiltViewModel()
    val windowSizeClass: WindowSizeClass = deriveWindowSizeClass()
    val layoutState by layoutController.layoutState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    // Get feature-specific detail content based on current feature
    val currentFeatureDetail: FeatureDetail? = when (layoutState.currentFeature) {
        Screens.StatsPill.route -> StatsPillFeatureDetail()
        Screens.Clip.route -> ClipFeatureDetail()
        Screens.Scratch.route -> ScratchFeatureDetail()
        Screens.Scenes.route -> ScenesFeatureDetail()
        else -> null
    }

    // Scroll behavior for TopAppBar
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // Handle keyboard shortcuts
    fun handleKeyEvent(event: android.view.KeyEvent): Boolean {
        when (event.keyCode) {
            android.view.KeyEvent.KEYCODE_M -> {
                layoutController.toggleNavigation()
                return true
            }
            android.view.KeyEvent.KEYCODE_D -> {
                layoutController.toggleDetail()
                return true
            }
            android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                val features = listOf("statspill", "clip", "scratch", "scenes")
                val currentIndex = features.indexOf(layoutState.currentFeature)
                val nextIndex = if (event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
                    (currentIndex + 1) % features.size
                } else {
                    if (currentIndex <= 0) features.size - 1 else currentIndex - 1
                }
                layoutController.selectFeature(features[nextIndex])
                return true
            }
        }
        return false
    }

    AlloyTheme(dynamicColor = dynamicColorEnabled) {
        Box(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .focusRequester(focusRequester)
        ) {
            // Request focus on launch for keyboard shortcuts
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            // TopAppBar for compact screens
            if (windowSizeClass == WindowSizeClass.COMPACT) {
                CenterAlignedTopAppBar(
                    title = { 
                        androidx.compose.material3.Text(
                            text = "Alloy Suite",
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { layoutController.openNavigation() }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open navigation"
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }

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
                detailContent = if (currentFeatureDetail?.showsDetailPane == true) {
                    {
                        DetailPane(
                            layoutController = layoutController,
                            title = layoutState.currentFeature.getScreenTitle() + " Settings",
                            detailContent = {
                                currentFeatureDetail.DetailContent()
                            }
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}

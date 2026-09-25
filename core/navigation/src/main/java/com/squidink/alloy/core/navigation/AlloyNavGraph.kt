package com.squidink.alloy.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * Main navigation graph for the Alloy app.
 *
 * Defines all routes and their corresponding composables.
 * Uses composable lambdas to avoid direct dependencies on feature modules.
 *
 * @param navController The navigation controller to use
 * @param startDestination The starting screen route
 * @param modifier Modifier to apply to the NavHost
 * @param onStatsPill Clickable composable for Stats Pill screen
 * @param onClip Clickable composable for Clip screen
 * @param onScratch Clickable composable for Scratch screen
 * @param onScenes Clickable composable for Scenes screen
 */
@Composable
fun AlloyNavGraph(
    navController: NavHostController,
    startDestination: String = Screens.START_DESTINATION,
    modifier: Modifier = Modifier,
    onStatsPill: @Composable () -> Unit,
    onClip: @Composable () -> Unit,
    onScratch: @Composable () -> Unit,
    onScenes: @Composable () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Stats Pill Screen
        composable(Screens.StatsPill.route) {
            onStatsPill()
        }
        
        // Clip Screen
        composable(Screens.Clip.route) {
            onClip()
        }
        
        // Scratch Screen
        composable(Screens.Scratch.route) {
            onScratch()
        }
        
        // Scenes Screen
        composable(Screens.Scenes.route) {
            onScenes()
        }
        
        // Clip Detail Screen (future use)
        composable(
            route = "clip_detail/{${Screens.ARG_CLIP_ID}}",
            arguments = listOf(
                navArgument(Screens.ARG_CLIP_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val clipId = backStackEntry.arguments?.getString(Screens.ARG_CLIP_ID)
            // TODO: Implement clip detail screen
        }
        
        // Scratch Detail Screen (future use)
        composable(
            route = "scratch_detail/{${Screens.ARG_SCRATCH_ID}}",
            arguments = listOf(
                navArgument(Screens.ARG_SCRATCH_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val scratchId = backStackEntry.arguments?.getString(Screens.ARG_SCRATCH_ID)
            // TODO: Implement scratch detail screen
        }
    }
}

/**
 * Extension function to navigate to a specific screen.
 */
fun NavHostController.navigate(screen: Screens) {
    navigate(screen.route)
}

/**
 * Extension function to navigate to a specific action.
 */
fun NavHostController.navigate(action: NavActions) {
    navigate(NavActions.getRoute(action))
}

package com.squidink.alloy.core.navigation.di

import androidx.navigation.NavHostController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

/**
 * DI module for navigation dependencies.
 */
@Module
@InstallIn(ActivityComponent::class)
object NavigationModule {
    
    /**
     * Provide NavHostController scoped to the activity.
     * Note: This is typically obtained from rememberNavController() in Compose.
     * This provider is for cases where you need it in non-Compose code.
     */
    @Provides
    @ActivityScoped
    fun provideNavController(): NavHostController {
        // This should be provided by rememberNavController() in the activity
        // This is a placeholder for DI purposes
        throw IllegalStateException(
            "NavController should be obtained via rememberNavController() in Compose. " +
            "Use AlloyNavGraph for navigation setup."
        )
    }
}

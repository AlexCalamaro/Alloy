package com.squidink.alloy.core.layout

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.squidink.alloy.core.datastore.DataStoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Repository for persisting and retrieving layout state.
 * Uses DataStore for type-safe, asynchronous preference storage.
 */
class LayoutStateRepository @Inject constructor(
    private val dataStoreManager: DataStoreManager
) {
    companion object {
        private const val KEY_NAVIGATION_PANE_OPEN = "navigation_pane_open"
        private const val KEY_DETAIL_PANE_OPEN = "detail_pane_open"
        private const val KEY_CURRENT_FEATURE = "current_feature"
        private const val KEY_DETAIL_FEATURE = "detail_feature"
    }

    /**
     * Observe the current layout state as a Flow.
     * Emits updates whenever the state changes.
     */
    fun observeLayoutState(): Flow<AppLayoutState> {
        val navigationFlow = dataStoreManager.getStringFlow(KEY_NAVIGATION_PANE_OPEN)
        val detailFlow = dataStoreManager.getStringFlow(KEY_DETAIL_PANE_OPEN)
        val currentFeatureFlow = dataStoreManager.getStringFlow(KEY_CURRENT_FEATURE)
        val detailFeatureFlow = dataStoreManager.getStringFlow(KEY_DETAIL_FEATURE)
        
        return combine(
            navigationFlow,
            detailFlow,
            currentFeatureFlow,
            detailFeatureFlow
        ) { navigation, detail, currentFeature, detailFeature ->
            AppLayoutState(
                navigationPaneOpen = navigation?.toBoolean() ?: false,
                detailPaneOpen = detail?.toBoolean() ?: false,
                currentFeature = currentFeature ?: "",
                detailFeature = detailFeature
            )
        }
    }

    /**
     * Get the current layout state synchronously.
     * Note: Use observeLayoutState() for reactive updates.
     */
    suspend fun getLayoutState(): AppLayoutState {
        return AppLayoutState(
            navigationPaneOpen = dataStoreManager.getStringFlow(KEY_NAVIGATION_PANE_OPEN).first()?.toBoolean() ?: false,
            detailPaneOpen = dataStoreManager.getStringFlow(KEY_DETAIL_PANE_OPEN).first()?.toBoolean() ?: false,
            currentFeature = dataStoreManager.getStringFlow(KEY_CURRENT_FEATURE).first() ?: "",
            detailFeature = dataStoreManager.getStringFlow(KEY_DETAIL_FEATURE).first()
        )
    }

    /**
     * Update the layout state.
     */
    suspend fun updateLayoutState(update: AppLayoutState.() -> AppLayoutState) {
        val currentState = getLayoutState()
        val newState = update(currentState)
        
        dataStoreManager.setString(KEY_NAVIGATION_PANE_OPEN, newState.navigationPaneOpen.toString())
        dataStoreManager.setString(KEY_DETAIL_PANE_OPEN, newState.detailPaneOpen.toString())
        dataStoreManager.setString(KEY_CURRENT_FEATURE, newState.currentFeature)
        dataStoreManager.setString(KEY_DETAIL_FEATURE, newState.detailFeature ?: "")
    }

    /**
     * Clear all persisted layout state.
     * Useful for resetting to defaults.
     */
    suspend fun clearLayoutState() {
        // Note: DataStoreManager doesn't have a remove method, so we clear by setting defaults
        dataStoreManager.setString(KEY_NAVIGATION_PANE_OPEN, "false")
        dataStoreManager.setString(KEY_DETAIL_PANE_OPEN, "false")
        dataStoreManager.setString(KEY_CURRENT_FEATURE, "")
        dataStoreManager.setString(KEY_DETAIL_FEATURE, "")
    }
}

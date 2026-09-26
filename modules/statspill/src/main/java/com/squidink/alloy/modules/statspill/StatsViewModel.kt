package com.squidink.alloy.modules.statspill

import android.content.Context
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.squidink.alloy.core.common.BaseViewModel
import com.squidink.alloy.core.common.UiAction
import com.squidink.alloy.core.common.UiEffect
import com.squidink.alloy.core.common.UiState
import com.squidink.alloy.core.data.repository.SettingsRepository
import com.squidink.alloy.core.domain.repository.BatteryInfo
import com.squidink.alloy.core.domain.repository.IStatsRepository
import com.squidink.alloy.core.domain.repository.NetStats
import com.squidink.alloy.core.permissions.AppPermission
import com.squidink.alloy.core.permissions.PermissionsManager
import com.squidink.alloy.core.proc.MemInfo
import com.squidink.alloy.modules.statspill.stats.OverlayServiceManager
import com.squidink.alloy.modules.statspill.stats.StatsDataObserver
import com.squidink.alloy.modules.statspill.stats.StatsSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for the Stats screen.
 *
 * Contains all data needed to render the stats UI including:
 * - Memory information
 * - CPU usage
 * - Network statistics
 * - Battery information
 * - UI flags (polling state, overlay state, etc.)
 */
data class StatsUiState(
    val memInfo: MemInfo = MemInfo(),
    val cpuUsagePercent: Float? = null,
    val netStats: NetStats = NetStats(),
    val batteryInfo: BatteryInfo = BatteryInfo(),
    val isLiveOverlayActive: Boolean = false,
    val isPolling: Boolean = false,
    val overlayServiceIntent: Intent? = null,
    val showPill: Boolean = true,
    val usePercentages: Boolean = true,
    val cornerPosition: CornerPosition = CornerPosition.TOP_RIGHT,
    val isLiveOverlayPermissionGranted: Boolean = false
) : UiState

/**
 * UI Actions for the Stats screen.
 *
 * Represents user interactions that can be performed:
 * - Toggle polling on/off
 * - Toggle live overlay
 * - Refresh data immediately
 * - Navigate to permission settings
 * - Dismiss permission dialogs
 */
sealed interface StatsUiAction : UiAction {
    data object TogglePolling : StatsUiAction

    data class ToggleLiveOverlay(
        val enable: Boolean,
    ) : StatsUiAction

    data object RefreshNow : StatsUiAction

    data object OpenOverlayPermissionSettings : StatsUiAction

    data object DismissPermissionDialog : StatsUiAction

    data class UpdateShowPill(val show: Boolean) : StatsUiAction

    data class UpdateUsePercentages(val usePercentages: Boolean) : StatsUiAction

    data class UpdateCornerPosition(val position: CornerPosition) : StatsUiAction
}

/**
 * UI Effects for the Stats screen.
 *
 * Represents one-shot side effects that should trigger UI actions:
 * - Show toast messages
 * - Navigate to permission settings
 */
sealed interface StatsUiEffect : UiEffect {
    data class ShowToast(val message: String) : StatsUiEffect

    data object OpenOverlayPermissionSettings : StatsUiEffect
}

/**
 * Corner position for the stats pill overlay.
 */
enum class CornerPosition(val displayName: String) {
    TOP_LEFT("Top-Left"),
    TOP_RIGHT("Top-Right"),
    BOTTOM_LEFT("Bottom-Left"),
    BOTTOM_RIGHT("Bottom-Right")
}

/**
 * Settings for the stats module.
 *
 * Contains user preferences for stats display and behavior.
 */
data class StatsSettings(
    val showPill: Boolean = true,
    val usePercentages: Boolean = true,
    val cornerPosition: CornerPosition = CornerPosition.TOP_RIGHT
)

/**
 * ViewModel for the Stats screen.
 *
 * Handles UI state management and user action dispatching.
 * Delegates data operations to managers:
 * - [StatsDataObserver] for system and battery statistics
 * - [StatsSettingsManager] for user preferences
 * - [OverlayServiceManager] for overlay service lifecycle
 *
 * This ViewModel follows MVI pattern with:
 * - UiState for reactive UI updates
 * - UiAction for user interactions
 * - UiEffect for one-shot side effects
 */
@HiltViewModel
class StatsViewModel
    @Inject
    constructor(
        private val statsRepository: IStatsRepository,
        private val settingsRepository: SettingsRepository,
        @ApplicationContext private val context: Context,
        private val permissionsManager: PermissionsManager,
    ) : BaseViewModel<StatsUiState, StatsUiAction, StatsUiEffect>(StatsUiState()) {

        // Delegate managers for separation of concerns
        private val dataObserver = StatsDataObserver(
            statsRepository = statsRepository,
            stateUpdater = { reducer -> updateState(reducer) }
        )

        private lateinit var settingsManager: StatsSettingsManager
        private lateinit var overlayManager: OverlayServiceManager

        init {
            // Initialize managers with scope
            settingsManager = StatsSettingsManager(
                settingsRepository = settingsRepository,
                stateUpdater = { reducer -> updateState(reducer) },
                scope = viewModelScope
            )

            overlayManager = OverlayServiceManager(
                context = context,
                permissionsManager = permissionsManager,
                effectEmitter = { effect -> sendEffect(effect) },
                stateUpdater = { reducer -> updateState(reducer) }
            )

            // Start observing data and settings
            dataObserver.startObserving(viewModelScope)
            settingsManager.startObserving()
            overlayManager.checkPermission()
        }

        override fun onAction(action: StatsUiAction) {
            when (action) {
                StatsUiAction.TogglePolling -> togglePolling()
                is StatsUiAction.ToggleLiveOverlay -> toggleOverlay(action.enable)
                StatsUiAction.RefreshNow -> refreshNow()
                StatsUiAction.OpenOverlayPermissionSettings -> openPermissionSettings()
                StatsUiAction.DismissPermissionDialog -> { /* no-op */ }
                is StatsUiAction.UpdateShowPill -> updateSettings(showPill = action.show)
                is StatsUiAction.UpdateUsePercentages -> updateSettings(usePercentages = action.usePercentages)
                is StatsUiAction.UpdateCornerPosition -> updateSettings(cornerPosition = action.position)
            }
        }

        private fun togglePolling() {
            val isPolling = uiState.value.isPolling
            updateState { it.copy(isPolling = !isPolling) }
            // Note: Repository handles actual polling via observeSystemStats()
        }

        private fun toggleOverlay(enable: Boolean) {
            if (enable) overlayManager.startOverlay() else overlayManager.stopOverlay()
        }

        private fun refreshNow() {
            viewModelScope.launch { statsRepository.pollSystemStats() }
        }

        private fun openPermissionSettings() {
            overlayManager.openPermissionSettings()
        }

        private fun updateSettings(
            showPill: Boolean? = null,
            usePercentages: Boolean? = null,
            cornerPosition: CornerPosition? = null
        ) {
            settingsManager.updateSettings(
                showPill = showPill,
                usePercentages = usePercentages,
                cornerPosition = cornerPosition,
                currentState = uiState.value
            )
        }

        override fun onCleared() {
            super.onCleared()
            overlayManager.stopOverlay()
        }
    }

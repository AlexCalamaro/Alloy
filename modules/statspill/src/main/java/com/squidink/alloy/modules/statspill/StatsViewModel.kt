package com.squidink.alloy.modules.statspill

import android.content.Context
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.squidink.alloy.core.common.BaseViewModel
import com.squidink.alloy.core.common.UiAction
import com.squidink.alloy.core.common.UiEffect
import com.squidink.alloy.core.common.UiState
import com.squidink.alloy.core.data.repository.SettingsRepository
import com.squidink.alloy.core.domain.repository.IStatsRepository
import com.squidink.alloy.core.permissions.AppPermission
import com.squidink.alloy.core.permissions.PermissionsManager
import com.squidink.alloy.core.proc.MemInfo
import com.squidink.alloy.core.proc.NetStats
import com.squidink.alloy.core.data.datasource.BatteryInfo
import com.squidink.alloy.modules.statspill.data.StatsRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
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
 * Delegates data operations to repositories:
 * - [StatsRepositoryImpl] for system and battery statistics
 * - [SettingsRepository] for user preferences
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
        private val statsRepositoryImpl: StatsRepositoryImpl,
        private val settingsRepository: SettingsRepository,
        @ApplicationContext private val context: Context,
        private val permissionsManager: PermissionsManager,
    ) : BaseViewModel<StatsUiState, StatsUiAction, StatsUiEffect>(StatsUiState()) {

        private var pollingJob: Job? = null
        private var batteryReceiver: android.content.BroadcastReceiver? = null
        private var serviceJob: Job? = null

        init {
            // Start observing data streams
            observeSystemStats()
            observeBatteryInfo()
            observeNetworkStats()
            loadSettings()
        }

        /**
         * Observe system statistics from the repository.
         * Updates UI state when new stats are available.
         */
        private fun observeSystemStats() {
            viewModelScope.launch {
                statsRepository.observeSystemStats().collectLatest { stats ->
                    updateState { currentState ->
                        currentState.copy(
                            memInfo = MemInfo(
                                totalMemKb = stats.memoryTotalBytes / 1024,
                                freeMemKb = (stats.memoryTotalBytes - stats.memoryUsedBytes) / 1024,
                                availableMemKb = (stats.memoryTotalBytes - stats.memoryUsedBytes) / 1024
                            ),
                            cpuUsagePercent = stats.cpuPercent
                        )
                    }
                }
            }
        }

        /**
         * Observe battery information from the repository.
         * Updates UI state when battery changes.
         */
        private fun observeBatteryInfo() {
            viewModelScope.launch {
                statsRepositoryImpl.observeBatteryInfo().collectLatest { batteryInfo ->
                    updateState { currentState ->
                        currentState.copy(batteryInfo = batteryInfo)
                    }
                }
            }
        }

        /**
         * Observe network statistics from the repository.
         * Updates UI state when network stats change.
         */
        private fun observeNetworkStats() {
            viewModelScope.launch {
                statsRepositoryImpl.observeNetworkStats().collectLatest { netStats ->
                    updateState { currentState ->
                        currentState.copy(netStats = netStats)
                    }
                }
            }
        }

        /**
         * Load settings from the settings repository.
         * Observes settings changes and updates UI state accordingly.
         */
        private fun loadSettings() {
            viewModelScope.launch {
                settingsRepository.observeShowPill().collectLatest { showPill ->
                    updateState { it.copy(showPill = showPill) }
                }
            }

            viewModelScope.launch {
                settingsRepository.observeUsePercentages().collectLatest { usePercentages ->
                    updateState { it.copy(usePercentages = usePercentages) }
                }
            }

            viewModelScope.launch {
                settingsRepository.observeCornerPosition().collectLatest { positionName ->
                    val position = try {
                        CornerPosition.valueOf(positionName)
                    } catch (e: IllegalArgumentException) {
                        CornerPosition.TOP_RIGHT
                    }
                    updateState { it.copy(cornerPosition = position) }
                }
            }

            // Check overlay permission
            checkOverlayPermission()
        }

        /**
         * Check if overlay permission is granted.
         */
        private fun checkOverlayPermission() {
            val isGranted = permissionsManager.isPermissionGranted(
                context,
                AppPermission.SystemOverlay
            )
            updateState { it.copy(isLiveOverlayPermissionGranted = isGranted) }
        }

        /**
         * Update settings through the settings repository.
         *
         * @param newSettings The updated settings
         */
        fun updateSettings(
            showPill: Boolean = uiState.value.showPill,
            usePercentages: Boolean = uiState.value.usePercentages,
            cornerPosition: CornerPosition = uiState.value.cornerPosition
        ) {
            val newShowPill = showPill
            val newUsePercentages = usePercentages
            val newCornerPosition = cornerPosition

            updateState {
                it.copy(
                    showPill = newShowPill,
                    usePercentages = newUsePercentages,
                    cornerPosition = newCornerPosition
                )
            }

            viewModelScope.launch {
                settingsRepository.setShowPill(newShowPill)
                settingsRepository.setUsePercentages(newUsePercentages)
                settingsRepository.setCornerPosition(newCornerPosition.name)
            }
        }

        override fun onAction(action: StatsUiAction) {
            when (action) {
                StatsUiAction.TogglePolling -> {
                    if (uiState.value.isPolling) stopPolling() else startPolling()
                }

                is StatsUiAction.ToggleLiveOverlay -> {
                    if (action.enable) {
                        startOverlayService()
                    } else {
                        stopOverlayService()
                    }
                }

                StatsUiAction.RefreshNow -> {
                    viewModelScope.launch {
                        statsRepository.pollSystemStats()
                    }
                }

                StatsUiAction.OpenOverlayPermissionSettings -> {
                    permissionsManager.openPermissionSettings(
                        context,
                        AppPermission.SystemOverlay
                    )
                }

                StatsUiAction.DismissPermissionDialog -> {
                    // Just dismiss, no action needed
                }

                is StatsUiAction.UpdateShowPill -> {
                    updateSettings(showPill = action.show)
                }

                is StatsUiAction.UpdateUsePercentages -> {
                    updateSettings(usePercentages = action.usePercentages)
                }

                is StatsUiAction.UpdateCornerPosition -> {
                    updateSettings(cornerPosition = action.position)
                }
            }
        }

        /**
         * Start polling system statistics.
         *
         * Polling is handled by the repository data source.
         * This ViewModel just controls the polling state.
         */
        fun startPolling() {
            if (pollingJob?.isActive == true) return
            updateState { it.copy(isPolling = true) }
            // Repository already handles the polling loop via observeSystemStats()
            // This is just a flag to indicate polling is active
        }

        /**
         * Stop polling system statistics.
         */
        fun stopPolling() {
            pollingJob?.cancel()
            pollingJob = null
            updateState { it.copy(isPolling = false) }
        }

        /**
         * Start the overlay service for live stats display.
         */
        private fun startOverlayService() {
            if (!permissionsManager.isPermissionGranted(
                    context,
                    AppPermission.SystemOverlay
                )
            ) {
                sendEffect(StatsUiEffect.OpenOverlayPermissionSettings)
                updateState { it.copy(isLiveOverlayActive = false) }
                return
            }

            val intent = Intent(context, StatsPillOverlayService::class.java)
            context.startForegroundService(intent)
            updateState { it.copy(isLiveOverlayActive = true, overlayServiceIntent = intent) }
        }

        /**
         * Stop the overlay service.
         */
        private fun stopOverlayService() {
            val intent = uiState.value.overlayServiceIntent
                ?: Intent(context, StatsPillOverlayService::class.java)
            context.stopService(intent)
            updateState { it.copy(isLiveOverlayActive = false, overlayServiceIntent = null) }
        }

        override fun onCleared() {
            super.onCleared()
            stopPolling()
            serviceJob?.cancel()
            serviceJob = null
            try {
                batteryReceiver?.let { context.unregisterReceiver(it) }
            } catch (e: Exception) {
                // Already unregistered
            }
        }
    }

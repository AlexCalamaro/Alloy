package com.squidink.alloy.modules.statspill

import androidx.lifecycle.viewModelScope
import com.squidink.alloy.core.common.BaseViewModel
import com.squidink.alloy.core.common.UiAction
import com.squidink.alloy.core.common.UiEffect
import com.squidink.alloy.core.common.UiState
import com.squidink.alloy.core.data.repository.SettingsRepository
import com.squidink.alloy.modules.statspill.domain.model.BatteryInfo
import com.squidink.alloy.modules.statspill.domain.model.CombinedTelemetry
import com.squidink.alloy.modules.statspill.domain.model.DiskStats
import com.squidink.alloy.modules.statspill.domain.model.NetworkStats
import com.squidink.alloy.modules.statspill.domain.model.StatCategory
import com.squidink.alloy.modules.statspill.domain.model.StatError
import com.squidink.alloy.modules.statspill.domain.model.SystemStats
import com.squidink.alloy.modules.statspill.domain.model.ThermalStats
import com.squidink.alloy.modules.statspill.domain.usecase.ObserveErrorsUseCase
import com.squidink.alloy.modules.statspill.domain.usecase.ObserveTelemetryUseCase
import com.squidink.alloy.modules.statspill.domain.usecase.PollTelemetryUseCase
import com.squidink.alloy.modules.statspill.stats.OverlayServiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for the Stats screen.
 * Pure data class free of Android framework classes like [android.content.Intent].
 */
data class StatsUiState(
    val telemetry: CombinedTelemetry = CombinedTelemetry(),
    val errors: Map<StatCategory, StatError> = emptyMap(),
    val isLiveOverlayActive: Boolean = false,
    val isPolling: Boolean = true,
    val usePercentages: Boolean = true,
    val cornerPosition: CornerPosition = CornerPosition.TOP_RIGHT,
    val isLiveOverlayPermissionGranted: Boolean = false
) : UiState {
    val systemStats: SystemStats? get() = telemetry.systemStats
    val netStats: NetworkStats get() = telemetry.networkStats
    val batteryInfo: BatteryInfo get() = telemetry.batteryInfo
    val diskStats: DiskStats? get() = telemetry.diskStats
    val thermalStats: ThermalStats? get() = telemetry.thermalStats
}

/**
 * UI Actions for user interactions on the Stats screen.
 */
sealed interface StatsUiAction : UiAction {
    data object TogglePolling : StatsUiAction
    data class ToggleLiveOverlay(val enable: Boolean) : StatsUiAction
    data object RefreshNow : StatsUiAction
    data object OpenOverlayPermissionSettings : StatsUiAction
    data object DismissPermissionDialog : StatsUiAction
    data class UpdateUsePercentages(val usePercentages: Boolean) : StatsUiAction
    data class UpdateCornerPosition(val position: CornerPosition) : StatsUiAction
    data class ClearError(val category: StatCategory) : StatsUiAction
}

/**
 * UI Effects for one-shot events.
 */
sealed interface StatsUiEffect : UiEffect {
    data class ShowToast(val message: String) : StatsUiEffect
    data object OpenOverlayPermissionSettings : StatsUiEffect
}

/**
 * Corner position for the floating stats pill overlay.
 */
enum class CornerPosition(val displayName: String) {
    TOP_LEFT("Top-Left"),
    TOP_RIGHT("Top-Right"),
    BOTTOM_LEFT("Bottom-Left"),
    BOTTOM_RIGHT("Bottom-Right")
}

/**
 * Production-grade MVI ViewModel for system telemetry monitoring.
 * Serves as the architectural reference pattern for Alloy feature modules.
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val observeTelemetryUseCase: ObserveTelemetryUseCase,
    private val pollTelemetryUseCase: PollTelemetryUseCase,
    private val observeErrorsUseCase: ObserveErrorsUseCase,
    private val settingsRepository: SettingsRepository,
    private val overlayManager: OverlayServiceManager
) : BaseViewModel<StatsUiState, StatsUiAction, StatsUiEffect>(StatsUiState()) {

    init {
        checkOverlayPermission()
        observeTelemetry()
        observeErrors()
        observeSettings()
    }

    override fun onAction(action: StatsUiAction) {
        when (action) {
            StatsUiAction.TogglePolling -> togglePolling()
            is StatsUiAction.ToggleLiveOverlay -> toggleOverlay(action.enable)
            StatsUiAction.RefreshNow -> refreshNow()
            StatsUiAction.OpenOverlayPermissionSettings -> overlayManager.openPermissionSettings()
            StatsUiAction.DismissPermissionDialog -> { /* no-op */ }
            is StatsUiAction.UpdateUsePercentages -> updateUsePercentages(action.usePercentages)
            is StatsUiAction.UpdateCornerPosition -> updateCornerPosition(action.position)
            is StatsUiAction.ClearError -> clearError(action.category)
        }
    }

    private fun checkOverlayPermission() {
        val isGranted = overlayManager.isPermissionGranted()
        updateState { it.copy(isLiveOverlayPermissionGranted = isGranted) }
    }

    private fun observeTelemetry() {
        viewModelScope.launch {
            observeTelemetryUseCase().collect { telemetry ->
                if (uiState.value.isPolling) {
                    updateState { it.copy(telemetry = telemetry) }
                }
            }
        }
    }

    private fun observeErrors() {
        viewModelScope.launch {
            observeErrorsUseCase().collect { error ->
                updateState { currentState ->
                    val newErrors = currentState.errors.toMutableMap()
                    newErrors[error.category] = error
                    currentState.copy(errors = newErrors)
                }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.observeUsePercentages().collect { usePercentages ->
                updateState { it.copy(usePercentages = usePercentages) }
            }
        }

        viewModelScope.launch {
            settingsRepository.observeCornerPosition().collect { positionName ->
                val position = try {
                    CornerPosition.valueOf(positionName)
                } catch (e: IllegalArgumentException) {
                    CornerPosition.TOP_RIGHT
                }
                updateState { it.copy(cornerPosition = position) }
            }
        }
    }

    private fun togglePolling() {
        val newPolling = !uiState.value.isPolling
        updateState { it.copy(isPolling = newPolling) }
    }

    private fun toggleOverlay(enable: Boolean) {
        if (enable) {
            val granted = overlayManager.isPermissionGranted()
            updateState { it.copy(isLiveOverlayPermissionGranted = granted) }
            if (!granted) {
                sendEffect(StatsUiEffect.OpenOverlayPermissionSettings)
                updateState { it.copy(isLiveOverlayActive = false) }
            } else {
                val started = overlayManager.startOverlay()
                updateState { it.copy(isLiveOverlayActive = started) }
            }
        } else {
            overlayManager.stopOverlay()
            updateState { it.copy(isLiveOverlayActive = false) }
        }
    }

    private fun refreshNow() {
        viewModelScope.launch {
            try {
                val freshTelemetry = pollTelemetryUseCase()
                updateState { it.copy(telemetry = freshTelemetry) }
            } catch (e: Exception) {
                sendEffect(StatsUiEffect.ShowToast("Refresh failed: ${e.message}"))
            }
        }
    }

    private fun updateUsePercentages(usePercentages: Boolean) {
        updateState { it.copy(usePercentages = usePercentages) }
        viewModelScope.launch {
            settingsRepository.setUsePercentages(usePercentages)
        }
    }

    private fun updateCornerPosition(position: CornerPosition) {
        updateState { it.copy(cornerPosition = position) }
        viewModelScope.launch {
            settingsRepository.setCornerPosition(position.name)
        }
    }

    private fun clearError(category: StatCategory) {
        updateState { currentState ->
            val newErrors = currentState.errors.toMutableMap()
            newErrors.remove(category)
            currentState.copy(errors = newErrors)
        }
    }
}

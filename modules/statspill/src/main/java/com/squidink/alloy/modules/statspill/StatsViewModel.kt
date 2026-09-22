package com.squidink.alloy.modules.statspill

import androidx.lifecycle.viewModelScope
import com.squidink.alloy.core.common.BaseViewModel
import com.squidink.alloy.core.common.UiAction
import com.squidink.alloy.core.common.UiEffect
import com.squidink.alloy.core.common.UiState
import com.squidink.alloy.core.proc.MemInfo
import com.squidink.alloy.core.proc.ProcReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class StatsUiState(
    val memInfo: MemInfo = MemInfo(),
    val cpuUsagePercent: Float? = null,
    val isLiveOverlayActive: Boolean = false,
    val isPolling: Boolean = false,
    val isThermalAlert: Boolean = false
) : UiState

sealed interface StatsUiAction : UiAction {
    data object TogglePolling : StatsUiAction
    data class ToggleLiveOverlay(val enable: Boolean) : StatsUiAction
    data object RefreshNow : StatsUiAction
}

sealed interface StatsUiEffect : UiEffect {
    data class ShowThermalWarning(val message: String) : StatsUiEffect
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val procReader: ProcReader,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<StatsUiState, StatsUiAction, StatsUiEffect>(StatsUiState()) {

    private var pollingJob: Job? = null

    init {
        startPolling()
    }

    override fun onAction(action: StatsUiAction) {
        when (action) {
            StatsUiAction.TogglePolling -> {
                if (uiState.value.isPolling) stopPolling() else startPolling()
            }
            is StatsUiAction.ToggleLiveOverlay -> {
                updateState { it.copy(isLiveOverlayActive = action.enable) }
            }
            StatsUiAction.RefreshNow -> {
                viewModelScope.launch { pollVitals() }
            }
        }
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        updateState { it.copy(isPolling = true) }
        pollingJob = viewModelScope.launch {
            while (true) {
                pollVitals()
                delay(1000L) // 1Hz telemetry polling
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        updateState { it.copy(isPolling = false) }
    }

    private suspend fun pollVitals() {
        val mem = withContext(ioDispatcher) { procReader.readMemInfo() }
        val cpu = withContext(ioDispatcher) { procReader.readCpuUsagePercent() }
        updateState { currentState ->
            currentState.copy(
                memInfo = mem,
                cpuUsagePercent = cpu
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}

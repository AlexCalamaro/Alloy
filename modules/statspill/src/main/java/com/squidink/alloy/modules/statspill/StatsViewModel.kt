package com.squidink.alloy.modules.statspill

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squidink.alloy.core.common.BaseViewModel
import com.squidink.alloy.core.common.UiAction
import com.squidink.alloy.core.common.UiEffect
import com.squidink.alloy.core.common.UiState
import com.squidink.alloy.core.common.di.IoDispatcher
import com.squidink.alloy.core.proc.MemInfo
import com.squidink.alloy.core.proc.ProcReader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class BatteryInfo(
    val level: Int = 0,
    val scale: Int = 100,
    val percentage: Int = 0,
    val health: Int = BatteryManager.BATTERY_HEALTH_UNKNOWN,
    val status: Int = BatteryManager.BATTERY_STATUS_UNKNOWN,
    val temperature: Int = 0, // tenths of a degree Celsius
    val voltage: Int = 0, // millivolts
    val isCharging: Boolean = false,
) {
    fun getTemperatureCelsius(): Float = temperature / 10f

    fun getHealthString(): String =
        when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Overvoltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            else -> "Unknown"
        }

    fun getStatusString(): String =
        when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }
}

data class StatsUiState(
    val memInfo: MemInfo = MemInfo(),
    val cpuUsagePercent: Float? = null,
    val batteryInfo: BatteryInfo = BatteryInfo(),
    val isLiveOverlayActive: Boolean = false,
    val isPolling: Boolean = false,
) : UiState

sealed interface StatsUiAction : UiAction {
    data object TogglePolling : StatsUiAction

    data class ToggleLiveOverlay(
        val enable: Boolean,
    ) : StatsUiAction

    data object RefreshNow : StatsUiAction
}

sealed interface StatsUiEffect : UiEffect {
    data class ShowToast(
        val message: String,
    ) : StatsUiEffect
}

@HiltViewModel
class StatsViewModel
    @Inject
    constructor(
        private val procReader: ProcReader,
        @ApplicationContext private val context: Context,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : BaseViewModel<StatsUiState, StatsUiAction, StatsUiEffect>(StatsUiState()) {
        private var pollingJob: Job? = null
        private var batteryReceiver: android.content.BroadcastReceiver? = null

        init {
            startPolling()
            registerBatteryReceiver()
        }

        private fun registerBatteryReceiver() {
            batteryReceiver =
                object : android.content.BroadcastReceiver() {
                    override fun onReceive(
                        context: Context,
                        intent: Intent,
                    ) {
                        val batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                        val batteryScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                        val batteryHealth = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
                        val batteryStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
                        val batteryTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                        val batteryVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)

                        updateState { currentState ->
                            currentState.copy(
                                batteryInfo =
                                    BatteryInfo(
                                        level = batteryLevel,
                                        scale = batteryScale,
                                        percentage = (batteryLevel * 100 / batteryScale),
                                        health = batteryHealth,
                                        status = batteryStatus,
                                        temperature = batteryTemp,
                                        voltage = batteryVoltage,
                                        isCharging = batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING,
                                    ),
                            )
                        }
                    }
                }

            try {
                val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                context.registerReceiver(batteryReceiver, filter)
            } catch (e: Exception) {
                // Receiver registration may fail in some contexts
            }
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
            pollingJob =
                viewModelScope.launch {
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
                    cpuUsagePercent = cpu,
                )
            }
        }

        override fun onCleared() {
            super.onCleared()
            stopPolling()
            try {
                batteryReceiver?.let { context.unregisterReceiver(it) }
            } catch (e: Exception) {
                // Already unregistered
            }
        }
    }

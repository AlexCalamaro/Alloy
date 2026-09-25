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
import com.squidink.alloy.core.domain.repository.IStatsRepository
import com.squidink.alloy.core.permissions.AppPermission
import com.squidink.alloy.core.permissions.PermissionsManager
import com.squidink.alloy.core.proc.MemInfo
import com.squidink.alloy.core.proc.ProcReader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val netStats: com.squidink.alloy.core.proc.NetStats = com.squidink.alloy.core.proc.NetStats(),
    val batteryInfo: BatteryInfo = BatteryInfo(),
    val isLiveOverlayActive: Boolean = false,
    val isPolling: Boolean = false,
    val overlayServiceIntent: Intent? = null,
) : UiState

sealed interface StatsUiAction : UiAction {
    data object TogglePolling : StatsUiAction

    data class ToggleLiveOverlay(
        val enable: Boolean,
    ) : StatsUiAction

    data object RefreshNow : StatsUiAction
    
    data object OpenOverlayPermissionSettings : StatsUiAction
    
    data object DismissPermissionDialog : StatsUiAction
}

sealed interface StatsUiEffect : UiEffect {
    data class ShowToast(
        val message: String,
    ) : StatsUiEffect
    
    data object OpenOverlayPermissionSettings : StatsUiEffect
}

@HiltViewModel
class StatsViewModel
    @Inject
    constructor(
        private val statsRepository: IStatsRepository,
        private val procReader: ProcReader,
        @ApplicationContext private val context: Context,
        private val permissionsManager: PermissionsManager,
    ) : BaseViewModel<StatsUiState, StatsUiAction, StatsUiEffect>(StatsUiState()) {
        private var pollingJob: Job? = null
        private var batteryReceiver: android.content.BroadcastReceiver? = null
        private var serviceJob: Job? = null

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
                    if (action.enable) {
                        startOverlayService()
                    } else {
                        stopOverlayService()
                    }
                }

                StatsUiAction.RefreshNow -> {
                    viewModelScope.launch { pollVitals() }
                }
                
                StatsUiAction.OpenOverlayPermissionSettings -> {
                    permissionsManager.openPermissionSettings(context, AppPermission.SystemOverlay)
                }
                
                StatsUiAction.DismissPermissionDialog -> {
                    // Just dismiss, no action needed
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

        private fun startOverlayService() {
            // Check overlay permission using PermissionsManager
            if (!permissionsManager.isPermissionGranted(context, AppPermission.SystemOverlay)) {
                sendEffect(StatsUiEffect.OpenOverlayPermissionSettings)
                updateState { it.copy(isLiveOverlayActive = false) }
                return
            }

            val intent = Intent(context, StatsPillOverlayService::class.java)
            context.startForegroundService(intent)
            updateState { it.copy(isLiveOverlayActive = true, overlayServiceIntent = intent) }
        }

        private fun stopOverlayService() {
            val intent = uiState.value.overlayServiceIntent ?: Intent(context, StatsPillOverlayService::class.java)
            context.stopService(intent)
            updateState { it.copy(isLiveOverlayActive = false, overlayServiceIntent = null) }
        }

        private suspend fun pollVitals() {
            val stats = statsRepository.pollSystemStats()
            val netStats = procReader.readNetworkStats()
            
            updateState { currentState ->
                currentState.copy(
                    memInfo = MemInfo(
                        totalMemKb = stats.memoryTotalBytes / 1024,
                        freeMemKb = (stats.memoryTotalBytes - stats.memoryUsedBytes) / 1024,
                        availableMemKb = (stats.memoryTotalBytes - stats.memoryUsedBytes) / 1024
                    ),
                    cpuUsagePercent = stats.cpuPercent,
                    netStats = netStats
                )
            }
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

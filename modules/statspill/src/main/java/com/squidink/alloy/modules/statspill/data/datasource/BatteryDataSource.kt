package com.squidink.alloy.core.data.datasource

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.modules.statspill.domain.model.BatteryInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for battery information.
 * Uses Android's BatteryManager to provide real-time battery state updates.
 */
@Singleton
class BatteryDataSource @Inject constructor(
    private val context: Context
) : StatDataSource<BatteryInfo> {

    private val tag = "BatteryDataSource"
    private val _batteryInfo = MutableStateFlow(readInternal())

    /**
     * Read current battery information.
     */
    override suspend fun read(): BatteryInfo = withContext(Dispatchers.IO) {
        readInternal()
    }

    private fun readInternal(): BatteryInfo {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            
            BatteryInfo(
                timestamp = System.currentTimeMillis(),
                level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
                scale = 100,
                percentage = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
                health = BatteryManager.BATTERY_HEALTH_UNKNOWN,
                status = if (batteryManager.isCharging) 
                    BatteryManager.BATTERY_STATUS_CHARGING 
                else 
                    BatteryManager.BATTERY_STATUS_DISCHARGING,
                temperature = 0,
                voltage = 0,
                isCharging = batteryManager.isCharging
            )
        } catch (e: Exception) {
            Logger.e(tag, "Error reading battery info", e)
            BatteryInfo()
        }
    }

    /**
     * Observe battery information as a continuous flow.
     * Updates are emitted when battery state changes.
     */
    override fun observe(): Flow<BatteryInfo> = _batteryInfo.asStateFlow()

    /**
     * Register for battery change broadcasts to receive real-time updates.
     * Call this when starting observation to enable live updates.
     *
     * @return The registered BroadcastReceiver
     */
    fun registerBatteryReceiver(): android.content.BroadcastReceiver {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                updateBatteryInfo(intent)
            }
        }

        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(receiver, filter)
        } catch (e: Exception) {
            Logger.e(tag, "Failed to register battery receiver", e)
        }

        return receiver
    }

    /**
     * Unregister the battery receiver.
     * Call this when stopping observation to prevent leaks.
     *
     * @param receiver The receiver to unregister
     */
    fun unregisterBatteryReceiver(receiver: android.content.BroadcastReceiver) {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            Logger.e(tag, "Failed to unregister battery receiver", e)
        }
    }

    private fun updateBatteryInfo(intent: Intent) {
        val batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val batteryScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val batteryHealth = intent.getIntExtra(
            BatteryManager.EXTRA_HEALTH,
            BatteryManager.BATTERY_HEALTH_UNKNOWN
        )
        val batteryStatus = intent.getIntExtra(
            BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN
        )
        val batteryTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val batteryVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)

        val batteryInfo = BatteryInfo(
            timestamp = System.currentTimeMillis(),
            level = batteryLevel,
            scale = batteryScale,
            percentage = (batteryLevel * 100 / batteryScale),
            health = batteryHealth,
            status = batteryStatus,
            temperature = batteryTemp,
            voltage = batteryVoltage,
            isCharging = batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING
        )

        _batteryInfo.value = batteryInfo
    }
}

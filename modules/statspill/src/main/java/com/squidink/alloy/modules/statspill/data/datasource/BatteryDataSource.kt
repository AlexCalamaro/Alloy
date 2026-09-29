package com.squidink.alloy.modules.statspill.data.datasource

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.modules.statspill.domain.model.BatteryInfo
import com.squidink.alloy.modules.statspill.domain.model.PluggedSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for battery information.
 * Uses Android's BatteryManager broadcast to provide reactive battery updates.
 */
@Singleton
class BatteryDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : StatDataSource<BatteryInfo> {

    /**
     * Read current battery information synchronously from sticky broadcast.
     */
    override suspend fun read(): BatteryInfo {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (intent != null) {
                parseBatteryIntent(intent)
            } else {
                readFromBatteryManager()
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error reading battery info synchronously", e)
            BatteryInfo()
        }
    }

    /**
     * Observe battery information as a reactive stream.
     * Registers a BroadcastReceiver while actively collected and automatically
     * cleans up when the collector cancels or leaves composition.
     */
    override fun observe(): Flow<BatteryInfo> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(parseBatteryIntent(intent))
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val stickyIntent = try {
            context.registerReceiver(receiver, filter)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to register battery receiver", e)
            null
        }

        // Emit initial sticky intent state if available
        if (stickyIntent != null) {
            trySend(parseBatteryIntent(stickyIntent))
        } else {
            trySend(readFromBatteryManager())
        }

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to unregister battery receiver", e)
            }
        }
    }.distinctUntilChanged()

    private fun readFromBatteryManager(): BatteryInfo {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
            val isCharging = batteryManager?.isCharging ?: false
            val currentUa = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)?.takeIf {
                it != Int.MIN_VALUE && it != 0
            }

            BatteryInfo(
                timestamp = System.currentTimeMillis(),
                level = level,
                scale = 100,
                percentage = level.coerceIn(0, 100),
                health = BatteryManager.BATTERY_HEALTH_UNKNOWN,
                status = if (isCharging) BatteryManager.BATTERY_STATUS_CHARGING else BatteryManager.BATTERY_STATUS_DISCHARGING,
                temperature = 0,
                voltage = 0,
                isCharging = isCharging,
                pluggedSource = if (isCharging) PluggedSource.UNKNOWN else PluggedSource.UNPLUGGED,
                currentMicroamperes = currentUa
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Error reading from BatteryManager", e)
            BatteryInfo()
        }
    }

    internal fun parseBatteryIntent(intent: Intent): BatteryInfo {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val percentage = ((level.toFloat() / scale.toFloat()) * 100f).toInt().coerceIn(0, 100)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val pluggedSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> PluggedSource.AC
            BatteryManager.BATTERY_PLUGGED_USB -> PluggedSource.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> PluggedSource.WIRELESS
            8 -> PluggedSource.DOCK // BATTERY_PLUGGED_DOCK (API 33+)
            0 -> PluggedSource.UNPLUGGED
            else -> if (isCharging) PluggedSource.UNKNOWN else PluggedSource.UNPLUGGED
        }

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val currentUa = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)?.takeIf {
            it != Int.MIN_VALUE && it != 0
        }

        return BatteryInfo(
            timestamp = System.currentTimeMillis(),
            level = level,
            scale = scale,
            percentage = percentage,
            health = health,
            status = status,
            temperature = temperature,
            voltage = voltage,
            isCharging = isCharging,
            pluggedSource = pluggedSource,
            currentMicroamperes = currentUa
        )
    }

    companion object {
        private const val TAG = "BatteryDataSource"
    }
}

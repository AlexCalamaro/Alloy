package com.squidink.alloy.modules.statspill.data.datasource

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.modules.statspill.domain.model.ThermalStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for thermal and temperature statistics.
 * Reads battery temperature via sticky battery broadcast.
 */
@Singleton
class ThermalDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : StatDataSource<ThermalStats> {

    /**
     * Read current thermal statistics.
     */
    override suspend fun read(): ThermalStats = withContext(Dispatchers.IO) {
        try {
            val batteryTemp = getBatteryTemperatureCelsius()

            ThermalStats(
                timestamp = System.currentTimeMillis(),
                batteryTemperature = batteryTemp
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Error reading thermal stats", e)
            ThermalStats(
                timestamp = System.currentTimeMillis()
            )
        }
    }

    /**
     * Observe thermal statistics as a flow.
     * Polls at 0.2Hz (every 5 seconds) as temperature shifts slowly.
     */
    override fun observe(): Flow<ThermalStats> = flow {
        while (true) {
            emit(read())
            delay(POLL_INTERVAL_MS)
        }
    }

    private fun getBatteryTemperatureCelsius(): Float? {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val tenthsOfCelsius = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            if (tenthsOfCelsius > 0) {
                tenthsOfCelsius / 10f
            } else {
                null
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error obtaining battery temperature", e)
            null
        }
    }

    companion object {
        private const val TAG = "ThermalDataSource"
        const val POLL_INTERVAL_MS = 5000L // 5 seconds
    }
}

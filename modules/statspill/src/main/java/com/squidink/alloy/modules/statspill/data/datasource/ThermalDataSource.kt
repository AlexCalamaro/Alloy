package com.squidink.alloy.core.data.datasource

import android.content.Context
import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.modules.statspill.domain.model.ThermalStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for thermal/temperature statistics.
 * Currently uses battery temperature from BatteryManager.
 * Note: Full system thermal access (CPU, skin temps) requires hidden APIs or root.
 */
@Singleton
class ThermalDataSource @Inject constructor(
    private val context: Context
) : StatDataSource<ThermalStats> {

    private val tag = "ThermalDataSource"

    /**
     * Read current thermal statistics.
     */
    override suspend fun read(): ThermalStats = withContext(Dispatchers.IO) {
        try {
            val batteryTemp = getBatteryTemperature()

            ThermalStats(
                timestamp = System.currentTimeMillis(),
                batteryTemperature = batteryTemp
            )
        } catch (e: Exception) {
            Logger.e(tag, "Error reading thermal stats", e)
            ThermalStats(
                timestamp = System.currentTimeMillis()
            )
        }
    }

    /**
     * Observe thermal statistics as a continuous flow.
     * Polls at 2Hz since temperature changes relatively slowly.
     */
    override fun observe(): Flow<ThermalStats> = flow {
        while (true) {
            emit(read())
            kotlinx.coroutines.delay(500) // 2Hz
        }
    }

    /**
     * Get battery temperature from BatteryManager.
     * Returns temperature in Celsius.
     * Note: Accurate system temperature readings require hidden APIs or root access.
     * Battery temperature can be obtained from battery broadcasts (see BatteryDataSource).
     */
    private fun getBatteryTemperature(): Float? {
        // Temperature is not directly available via public APIs.
        // BatteryDataSource provides temperature from battery broadcasts.
        // This method returns null to indicate unavailable data.
        return null
    }
}

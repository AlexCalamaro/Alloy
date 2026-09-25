package com.squidink.alloy.core.data.datasource

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.core.data.cache.MemoryCache
import com.squidink.alloy.core.proc.NetStats
import com.squidink.alloy.core.proc.SystemStatsReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class for battery information.
 */
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

    fun getHealthString(): String = when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Overvoltage"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
        else -> "Unknown"
    }

    fun getStatusString(): String = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
        else -> "Unknown"
    }
}

/**
 * Local data source for system statistics.
 *
 * Wraps SystemStatsReader to provide observable Flow of system stats.
 * Implements caching for improved performance.
 */
@Singleton
class SystemStatsDataSource @Inject constructor(
    private val systemStatsReader: SystemStatsReader,
    private val cache: MemoryCache<String, Any>
) : LocalDataSource<SystemStatsData, String> {

    private val _currentStats = MutableStateFlow(SystemStatsData())
    val currentStats: Flow<SystemStatsData> = _currentStats.asStateFlow()

    override suspend fun getById(id: String): SystemStatsData? {
        return when (id) {
            CACHE_KEY_CURRENT -> _currentStats.value
            else -> null
        }
    }

    override suspend fun getAll(): List<SystemStatsData> {
        return listOf(_currentStats.value)
    }

    override suspend fun insert(item: SystemStatsData): String {
        _currentStats.value = item
        cache.put(CACHE_KEY_CURRENT, item)
        return CACHE_KEY_CURRENT
    }

    override suspend fun update(item: SystemStatsData) {
        insert(item)
    }

    override suspend fun delete(id: String) {
        cache.remove(id)
    }

    override fun observeById(id: String): Flow<SystemStatsData?> {
        return when (id) {
            CACHE_KEY_CURRENT -> _currentStats.asStateFlow().map { it }
            else -> flow { }
        }
    }

    override fun observeAll(): Flow<List<SystemStatsData>> {
        return _currentStats.map { listOf(it) }
    }

    /**
     * Poll current system stats from the reader.
     *
     * @return The polled system stats data
     */
    suspend fun pollStats(): SystemStatsData = withContext(Dispatchers.IO) {
        try {
            val memInfo = systemStatsReader.readMemInfo()
            val cpuPercent = systemStatsReader.readCpuUsagePercent() ?: 0f
            val netStats = systemStatsReader.readNetworkStats()

            val totalBytes = memInfo.totalMemKb * 1024
            val availableBytes = memInfo.availableMemKb * 1024
            val usedBytes = totalBytes - availableBytes
            val memPercent = if (totalBytes > 0) {
                (usedBytes.toFloat() / totalBytes.toFloat()) * 100f
            } else 0f

            val stats = SystemStatsData(
                memoryUsedBytes = usedBytes,
                memoryTotalBytes = totalBytes,
                memoryPercent = memPercent,
                cpuPercent = cpuPercent,
                netStats = netStats,
                timestamp = System.currentTimeMillis()
            )

            _currentStats.value = stats
            stats
        } catch (e: Exception) {
            Logger.e(TAG, "Error polling system stats", e)
            SystemStatsData()
        }
    }

    companion object {
        private const val TAG = "SystemStatsDataSource"
        private const val CACHE_KEY_CURRENT = "current_stats"
        private const val CACHE_KEY_MEM_INFO = "mem_info"
        private const val CACHE_KEY_NET_STATS = "net_stats"
    }
}

/**
 * Data class containing system statistics.
 */
data class SystemStatsData(
    val memoryUsedBytes: Long = 0,
    val memoryTotalBytes: Long = 0,
    val memoryPercent: Float = 0f,
    val cpuPercent: Float = 0f,
    val netStats: NetStats = NetStats(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Remote data source for battery information.
 *
 * Uses Android's BatteryManager to fetch battery state.
 * This is considered "remote" because it queries the system service
 * rather than reading from local storage.
 */
@Singleton
class BatteryDataSource @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : RemoteDataSource<BatteryInfo, String> {

    private val _batteryInfo = MutableStateFlow(BatteryInfo())
    val batteryInfo: Flow<BatteryInfo> = _batteryInfo.asStateFlow()

    /**
     * Register for battery change broadcasts to receive real-time updates.
     */
    fun registerBatteryReceiver(): android.content.BroadcastReceiver {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
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

        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(receiver, filter)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to register battery receiver", e)
        }

        // Also fetch initial battery state
        updateBatteryInfo()
        return receiver
    }

    /**
     * Unregister the battery receiver.
     */
    fun unregisterBatteryReceiver(receiver: android.content.BroadcastReceiver) {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to unregister battery receiver", e)
        }
    }

    /**
     * Get current battery info immediately from the system.
     */
    fun getCurrentBatteryInfo(): BatteryInfo {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return BatteryInfo(
            level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
            scale = 100,
            percentage = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
            health = BatteryManager.BATTERY_HEALTH_UNKNOWN,
            status = if (batteryManager.isCharging) BatteryManager.BATTERY_STATUS_CHARGING 
                     else BatteryManager.BATTERY_STATUS_DISCHARGING,
            temperature = 0,
            voltage = 0,
            isCharging = batteryManager.isCharging
        )
    }

    private fun updateBatteryInfo() {
        val info = getCurrentBatteryInfo()
        _batteryInfo.value = info
    }

    override suspend fun fetchById(id: String): BatteryInfo? {
        return when (id) {
            CACHE_KEY_CURRENT -> _batteryInfo.value
            else -> null
        }
    }

    override suspend fun fetchAll(): List<BatteryInfo> {
        return listOf(_batteryInfo.value)
    }

    override suspend fun create(item: BatteryInfo): String {
        // Battery info is read-only, cannot create
        throw UnsupportedOperationException("Battery info is read-only")
    }

    override suspend fun update(item: BatteryInfo) {
        // Battery info is read-only, cannot update
        throw UnsupportedOperationException("Battery info is read-only")
    }

    override suspend fun delete(id: String) {
        // Battery info is read-only, cannot delete
        throw UnsupportedOperationException("Battery info is read-only")
    }

    companion object {
        private const val TAG = "BatteryDataSource"
        private const val CACHE_KEY_CURRENT = "current_battery"
    }
}

package com.squidink.alloy.modules.statspill.domain.repository

import com.squidink.alloy.modules.statspill.domain.model.BatteryInfo
import com.squidink.alloy.modules.statspill.domain.model.DiskStats
import com.squidink.alloy.modules.statspill.domain.model.NetworkStats
import com.squidink.alloy.modules.statspill.domain.model.StatCategory
import com.squidink.alloy.modules.statspill.domain.model.StatType
import com.squidink.alloy.modules.statspill.domain.model.SystemStats
import com.squidink.alloy.modules.statspill.domain.model.ThermalStats
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for system statistics operations.
 *
 * Provides both generic stat observation by category and type-safe convenience methods.
 */
interface IStatsRepository {
    /**
     * Observe all statistics as a map by category.
     *
     * @return Flow emitting a map of all stat categories to their current values
     */
    fun observeAllStats(): Flow<Map<StatCategory, StatType>>
    
    /**
     * Observe statistics for a specific category.
     *
     * @param category The stat category to observe
     * @return Flow emitting stat updates for the specified category
     */
    fun observeStats(category: StatCategory): Flow<StatType>
    
    /**
     * Poll all statistics immediately.
     *
     * @return Map of all stat categories to their current values
     */
    suspend fun pollAllStats(): Map<StatCategory, StatType>
    
    /**
     * Poll statistics for a specific category.
     *
     * @param category The stat category to poll
     * @return Current stat value for the specified category
     */
    suspend fun pollStats(category: StatCategory): StatType
    
    // Type-safe convenience methods for existing stat types
    
    /**
     * Observe system statistics (CPU, memory) as a Flow.
     *
     * @return Flow emitting system stats updates
     */
    fun observeSystemStats(): Flow<SystemStats>
    
    /**
     * Poll system statistics once.
     *
     * @return Current system statistics
     */
    suspend fun pollSystemStats(): SystemStats
    
    /**
     * Get memory usage percentage.
     *
     * @return Memory usage as percentage (0.0 to 100.0)
     */
    suspend fun getMemoryPercent(): Float
    
    /**
     * Get CPU usage percentage.
     *
     * @return CPU usage as percentage (0.0 to 100.0)
     */
    suspend fun getCpuPercent(): Float
    
    /**
     * Observe battery information.
     *
     * @return Flow emitting battery info updates
     */
    fun observeBatteryInfo(): Flow<BatteryInfo>
    
    /**
     * Observe network statistics.
     *
     * @return Flow emitting network stats updates
     */
    fun observeNetworkStats(): Flow<NetworkStats>
    
    /**
     * Observe disk/storage statistics.
     *
     * @return Flow emitting disk stats updates
     */
    fun observeDiskStats(): Flow<DiskStats>
    
    /**
     * Poll disk/storage statistics once.
     *
     * @return Current disk statistics
     */
    suspend fun pollDiskStats(): DiskStats
    
    /**
     * Observe thermal/temperature statistics.
     *
     * @return Flow emitting thermal stats updates
     */
    fun observeThermalStats(): Flow<ThermalStats>
    
    /**
     * Poll thermal/temperature statistics once.
     *
     * @return Current thermal statistics
     */
    suspend fun pollThermalStats(): ThermalStats
}

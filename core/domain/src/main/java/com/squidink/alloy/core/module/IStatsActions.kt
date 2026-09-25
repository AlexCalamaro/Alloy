package com.squidink.alloy.core.module

import com.squidink.alloy.core.domain.repository.SystemStats

/**
 * Interface for Stats module actions.
 *
 * Defines actions that can be triggered on the Stats module from other parts of the app.
 */
interface IStatsActions : IModuleActions {
    /**
     * Get current system statistics.
     */
    fun getSystemStats(): SystemStats
    
    /**
     * Start monitoring system stats.
     */
    fun startMonitoring()
    
    /**
     * Stop monitoring system stats.
     */
    fun stopMonitoring()
    
    /**
     * Share current system stats.
     */
    fun shareStats()
}

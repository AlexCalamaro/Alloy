package com.squidink.alloy.core.module


/**
 * Interface for Stats module actions.
 *
 * Defines actions that can be triggered on the Stats module from other parts of the app.
 */
interface IStatsActions : IModuleActions {
    
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

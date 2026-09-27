package com.squidink.alloy.modules.statspill.domain.model

/**
 * Base interface for all stat types.
 * Enables uniform handling in UI and repository layers.
 */
interface StatType {
    val id: String
    val name: String
    val category: StatCategory
    val timestamp: Long
}

/**
 * Categories for organizing different types of statistics.
 */
enum class StatCategory {
    SYSTEM,      // CPU, Memory
    POWER,       // Battery
    NETWORK,     // Network I/O
    STORAGE,     // Disk usage
    THERMAL,     // Temperature
    CUSTOM       // User-defined
}

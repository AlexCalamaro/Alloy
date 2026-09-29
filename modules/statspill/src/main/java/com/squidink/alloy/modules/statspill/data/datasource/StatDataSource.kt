package com.squidink.alloy.modules.statspill.data.datasource

import com.squidink.alloy.modules.statspill.domain.model.StatType
import kotlinx.coroutines.flow.Flow

/**
 * Generic interface for stat data sources.
 * Each telemetry type has its own dedicated data source implementing this interface.
 *
 * @param T The type of stat data this source provides (must implement [StatType])
 */
interface StatDataSource<T : StatType> {
    /**
     * Read the current stat value synchronously.
     *
     * @return The current stat value
     */
    suspend fun read(): T

    /**
     * Observe stat values as a continuous flow of updates.
     *
     * @return Flow emitting stat updates
     */
    fun observe(): Flow<T>
}

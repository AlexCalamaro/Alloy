package com.squidink.alloy.modules.statspill.stats

import com.squidink.alloy.modules.statspill.domain.model.StatCategory
import com.squidink.alloy.modules.statspill.domain.repository.IStatsRepository
import com.squidink.alloy.modules.statspill.StatsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Coordinates observation of all system statistics data streams.
 *
 * Handles:
 * - System stats (CPU, memory)
 * - Battery information
 * - Network statistics
 * - Disk statistics
 * - Thermal statistics
 *
 * Updates UI state via provided state updater function.
 */
class StatsDataObserver(
    private val statsRepository: IStatsRepository,
    private val stateUpdater: ((StatsUiState) -> StatsUiState) -> Unit
) {
    /**
     * Start observing all data streams in the given scope.
     */
    fun startObserving(scope: CoroutineScope) {
        observeSystemStats(scope)
        observeBatteryInfo(scope)
        observeNetworkStats(scope)
        observeDiskStats(scope)
        observeThermalStats(scope)
    }

    /**
     * Start observing a specific stat category.
     */
    fun startObservingCategory(category: StatCategory, scope: CoroutineScope) {
        when (category) {
            StatCategory.SYSTEM -> observeSystemStats(scope)
            StatCategory.POWER -> observeBatteryInfo(scope)
            StatCategory.NETWORK -> observeNetworkStats(scope)
            StatCategory.STORAGE -> observeDiskStats(scope)
            StatCategory.THERMAL -> observeThermalStats(scope)
            StatCategory.CUSTOM -> Unit // Not implemented yet
        }
    }

    private fun observeSystemStats(scope: CoroutineScope) {
        scope.launch {
            statsRepository.observeSystemStats().collectLatest { stats ->
                stateUpdater { currentState ->
                    currentState.copy(systemStats = stats)
                }
            }
        }
    }

    private fun observeBatteryInfo(scope: CoroutineScope) {
        scope.launch {
            statsRepository.observeBatteryInfo().collectLatest { batteryInfo ->
                stateUpdater { currentState ->
                    currentState.copy(batteryInfo = batteryInfo)
                }
            }
        }
    }

    private fun observeNetworkStats(scope: CoroutineScope) {
        scope.launch {
            statsRepository.observeNetworkStats().collectLatest { netStats ->
                stateUpdater { currentState ->
                    currentState.copy(netStats = netStats)
                }
            }
        }
    }

    private fun observeDiskStats(scope: CoroutineScope) {
        scope.launch {
            statsRepository.observeDiskStats().collectLatest { diskStats ->
                stateUpdater { currentState ->
                    currentState.copy(diskStats = diskStats)
                }
            }
        }
    }

    private fun observeThermalStats(scope: CoroutineScope) {
        scope.launch {
            statsRepository.observeThermalStats().collectLatest { thermalStats ->
                stateUpdater { currentState ->
                    currentState.copy(thermalStats = thermalStats)
                }
            }
        }
    }
}

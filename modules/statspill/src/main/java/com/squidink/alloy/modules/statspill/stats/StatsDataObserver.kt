package com.squidink.alloy.modules.statspill.stats

import com.squidink.alloy.core.domain.repository.IStatsRepository
import com.squidink.alloy.core.domain.repository.SystemStats
import com.squidink.alloy.core.proc.MemInfo
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
    }

    private fun observeSystemStats(scope: CoroutineScope) {
        scope.launch {
            statsRepository.observeSystemStats().collectLatest { stats ->
                stateUpdater { currentState ->
                    currentState.copy(
                        memInfo = MemInfo(
                            totalMemKb = stats.memoryTotalBytes / 1024,
                            freeMemKb = (stats.memoryTotalBytes - stats.memoryUsedBytes) / 1024,
                            availableMemKb = (stats.memoryTotalBytes - stats.memoryUsedBytes) / 1024
                        ),
                        cpuUsagePercent = stats.cpuPercent
                    )
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
}

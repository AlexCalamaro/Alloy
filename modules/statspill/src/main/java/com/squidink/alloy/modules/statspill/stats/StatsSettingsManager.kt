package com.squidink.alloy.modules.statspill.stats

import com.squidink.alloy.core.data.repository.SettingsRepository
import com.squidink.alloy.modules.statspill.CornerPosition
import com.squidink.alloy.modules.statspill.StatsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Manages stats-related settings lifecycle.
 *
 * Handles:
 * - Loading settings from DataStore
 * - Observing settings changes
 * - Persisting settings updates
 */
class StatsSettingsManager(
    private val settingsRepository: SettingsRepository,
    private val stateUpdater: ((StatsUiState) -> StatsUiState) -> Unit,
    private val scope: CoroutineScope
) {
    /**
     * Start observing all settings streams.
     */
    fun startObserving() {
        observeShowPill()
        observeUsePercentages()
        observeCornerPosition()
    }

    private fun observeShowPill() {
        scope.launch {
            settingsRepository.observeShowPill().collectLatest { showPill ->
                stateUpdater { currentState ->
                    currentState.copy(showPill = showPill)
                }
            }
        }
    }

    private fun observeUsePercentages() {
        scope.launch {
            settingsRepository.observeUsePercentages().collectLatest { usePercentages ->
                stateUpdater { currentState ->
                    currentState.copy(usePercentages = usePercentages)
                }
            }
        }
    }

    private fun observeCornerPosition() {
        scope.launch {
            settingsRepository.observeCornerPosition().collectLatest { positionName ->
                val position = try {
                    CornerPosition.valueOf(positionName)
                } catch (e: IllegalArgumentException) {
                    CornerPosition.TOP_RIGHT
                }
                stateUpdater { currentState ->
                    currentState.copy(cornerPosition = position)
                }
            }
        }
    }

    /**
     * Update settings and persist to DataStore.
     */
    fun updateSettings(
        showPill: Boolean? = null,
        usePercentages: Boolean? = null,
        cornerPosition: CornerPosition? = null,
        currentState: StatsUiState
    ) {
        val newShowPill = showPill ?: currentState.showPill
        val newUsePercentages = usePercentages ?: currentState.usePercentages
        val newCornerPosition = cornerPosition ?: currentState.cornerPosition

        stateUpdater { it ->
            it.copy(
                showPill = newShowPill,
                usePercentages = newUsePercentages,
                cornerPosition = newCornerPosition
            )
        }

        scope.launch {
            settingsRepository.setShowPill(newShowPill)
            settingsRepository.setUsePercentages(newUsePercentages)
            settingsRepository.setCornerPosition(newCornerPosition.name)
        }
    }
}

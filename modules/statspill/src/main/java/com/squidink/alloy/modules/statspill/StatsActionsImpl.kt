package com.squidink.alloy.modules.statspill

import com.squidink.alloy.core.feature.FeatureIds
import com.squidink.alloy.core.module.IStatsActions
import com.squidink.alloy.modules.statspill.domain.repository.IStatsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of IStatsActions enabling cross-module coordination.
 */
@Singleton
class StatsActionsImpl @Inject constructor(
    private val statsRepository: IStatsRepository
) : IStatsActions {

    override val moduleId: String = FeatureIds.STATS_PILL
    override val displayName: String = "Stats Vitals"
    override val screenRoute: String = "stats_pill"

    override fun startMonitoring() {
        // Module monitoring action
    }

    override fun stopMonitoring() {
        // Module monitoring action
    }

    override fun shareStats() {
        // Share stats action
    }
}

package com.squidink.alloy.core.domain.common.usecase.stats

import com.squidink.alloy.modules.statspill.domain.model.SystemStats
import com.squidink.alloy.core.domain.common.usecase.SimpleUseCase
import com.squidink.alloy.modules.statspill.domain.repository.IStatsRepository
import javax.inject.Inject

/**
 * Use case to calculate current system statistics.
 *
 * Business logic:
 * - Reads CPU and memory from system
 * - Calculates percentages
 * - Returns structured stats data
 *
 * This use case is pure and testable without Android dependencies.
 */
class CalculateSystemStatsUseCase @Inject constructor(
    private val statsRepository: IStatsRepository
) : SimpleUseCase<SystemStats> {
    
    override suspend fun execute(input: Unit): SystemStats {
        return statsRepository.pollSystemStats()
    }
}

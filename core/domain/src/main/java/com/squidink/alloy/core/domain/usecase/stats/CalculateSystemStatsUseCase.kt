package com.squidink.alloy.core.domain.usecase.stats

import com.squidink.alloy.core.domain.repository.IStatsRepository
import com.squidink.alloy.core.domain.repository.SystemStats
import com.squidink.alloy.core.domain.usecase.SimpleUseCase
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

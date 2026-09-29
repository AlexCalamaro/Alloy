package com.squidink.alloy.modules.statspill.domain.usecase

import com.squidink.alloy.core.domain.common.usecase.SimpleUseCase
import com.squidink.alloy.modules.statspill.domain.model.SystemStats
import com.squidink.alloy.modules.statspill.domain.repository.IStatsRepository
import javax.inject.Inject

/**
 * Use case to calculate current system statistics.
 *
 * Reads CPU and memory telemetry, returning structured SystemStats.
 */
class CalculateSystemStatsUseCase @Inject constructor(
    private val statsRepository: IStatsRepository
) : SimpleUseCase<SystemStats> {

    override suspend fun execute(input: Unit): SystemStats {
        return statsRepository.pollSystemStats()
    }

    suspend operator fun invoke(): SystemStats = execute(Unit)
}

package com.squidink.alloy.modules.statspill.domain.usecase

import com.squidink.alloy.modules.statspill.domain.model.StatCategory
import com.squidink.alloy.modules.statspill.domain.model.StatType
import com.squidink.alloy.modules.statspill.domain.repository.IStatsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for polling all statistics immediately.
 */
@Singleton
class PollAllStatsUseCase @Inject constructor(
    private val statsRepository: IStatsRepository
) {
    suspend operator fun invoke(): Map<StatCategory, StatType> {
        return statsRepository.pollAllStats()
    }
}

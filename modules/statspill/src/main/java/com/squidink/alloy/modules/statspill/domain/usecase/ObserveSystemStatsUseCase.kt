package com.squidink.alloy.modules.statspill.domain.usecase

import com.squidink.alloy.modules.statspill.domain.model.SystemStats
import com.squidink.alloy.modules.statspill.domain.repository.IStatsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing system statistics (CPU, RAM).
 */
class ObserveSystemStatsUseCase @Inject constructor(
    private val statsRepository: IStatsRepository
) {
    operator fun invoke(): Flow<SystemStats> {
        return statsRepository.observeSystemStats()
    }
}

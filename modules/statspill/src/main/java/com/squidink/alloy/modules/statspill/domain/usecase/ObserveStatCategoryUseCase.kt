package com.squidink.alloy.modules.statspill.domain.usecase

import com.squidink.alloy.modules.statspill.domain.model.StatCategory
import com.squidink.alloy.modules.statspill.domain.model.StatType
import com.squidink.alloy.modules.statspill.domain.repository.IStatsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing a specific stat category.
 */
class ObserveStatCategoryUseCase @Inject constructor(
    private val statsRepository: IStatsRepository
) {
    operator fun invoke(category: StatCategory): Flow<StatType> {
        return statsRepository.observeStats(category)
    }
}

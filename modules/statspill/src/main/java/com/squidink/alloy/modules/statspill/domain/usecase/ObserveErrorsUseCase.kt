package com.squidink.alloy.modules.statspill.domain.usecase

import com.squidink.alloy.modules.statspill.domain.model.StatError
import com.squidink.alloy.modules.statspill.domain.repository.IStatsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe telemetry and data source error streams.
 */
class ObserveErrorsUseCase @Inject constructor(
    private val statsRepository: IStatsRepository
) {
    operator fun invoke(): Flow<StatError> {
        return statsRepository.observeErrors()
    }
}

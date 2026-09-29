package com.squidink.alloy.modules.statspill.domain.usecase

import com.squidink.alloy.modules.statspill.domain.model.CombinedTelemetry
import com.squidink.alloy.modules.statspill.domain.repository.IStatsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe real-time consolidated system telemetry.
 */
class ObserveTelemetryUseCase @Inject constructor(
    private val statsRepository: IStatsRepository
) {
    operator fun invoke(): Flow<CombinedTelemetry> {
        return statsRepository.observeCombinedTelemetry()
    }
}

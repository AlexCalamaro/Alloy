package com.squidink.alloy.modules.statspill.domain.usecase

import com.squidink.alloy.modules.statspill.domain.model.CombinedTelemetry
import com.squidink.alloy.modules.statspill.domain.repository.IStatsRepository
import javax.inject.Inject

/**
 * Use case to poll all telemetry data immediately on demand.
 */
class PollTelemetryUseCase @Inject constructor(
    private val statsRepository: IStatsRepository
) {
    suspend operator fun invoke(): CombinedTelemetry {
        return statsRepository.pollCombinedTelemetry()
    }
}

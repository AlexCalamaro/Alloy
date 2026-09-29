package com.squidink.alloy.modules.statspill.domain.usecase

import app.cash.turbine.test
import com.squidink.alloy.modules.statspill.FakeStatsRepository
import com.squidink.alloy.modules.statspill.domain.model.ErrorType
import com.squidink.alloy.modules.statspill.domain.model.StatCategory
import com.squidink.alloy.modules.statspill.domain.model.StatError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class DomainUseCasesTest {

    private lateinit var fakeRepository: FakeStatsRepository

    @Before
    fun setUp() {
        fakeRepository = FakeStatsRepository()
    }

    @Test
    fun `ObserveTelemetryUseCase emits repository telemetry stream`() = runTest {
        val useCase = ObserveTelemetryUseCase(fakeRepository)

        useCase().test {
            val telemetry = awaitItem()
            assertNotNull(telemetry.systemStats)
            assertEquals(25.5f, telemetry.systemStats?.cpuPercent)
            awaitComplete()
        }
    }

    @Test
    fun `PollTelemetryUseCase returns current telemetry snapshot`() = runTest {
        val useCase = PollTelemetryUseCase(fakeRepository)

        val result = useCase()
        assertEquals(37.5f, result.systemStats?.memoryPercent)
        assertEquals(80, result.batteryInfo.percentage)
    }

    @Test
    fun `ObserveErrorsUseCase emits errors as they occur`() = runTest {
        val useCase = ObserveErrorsUseCase(fakeRepository)

        useCase().test {
            val testError = StatError(
                category = StatCategory.NETWORK,
                type = ErrorType.TIMEOUT,
                message = "Network poll timed out"
            )
            fakeRepository.emitError(testError)

            val emitted = awaitItem()
            assertEquals(StatCategory.NETWORK, emitted.category)
            assertEquals(ErrorType.TIMEOUT, emitted.type)
        }
    }

    @Test
    fun `CalculateSystemStatsUseCase computes system stats`() = runTest {
        val useCase = CalculateSystemStatsUseCase(fakeRepository)

        val stats = useCase()
        assertEquals(25.5f, stats.cpuPercent)
        assertEquals(37.5f, stats.memoryPercent)
    }
}

package com.squidink.alloy.modules.statspill

import com.squidink.alloy.core.domain.repository.IStatsRepository
import com.squidink.alloy.core.domain.repository.SystemStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeStatsRepository : IStatsRepository {
    override fun observeSystemStats(): Flow<SystemStats> = flowOf(
        SystemStats(
            memoryUsedBytes = 6000000 * 1024,
            memoryTotalBytes = 16000000 * 1024,
            memoryPercent = 37.5f,
            cpuPercent = 25.5f,
            timestamp = System.currentTimeMillis()
        )
    )
    
    override suspend fun pollSystemStats(): SystemStats {
        return SystemStats(
            memoryUsedBytes = 6000000 * 1024,
            memoryTotalBytes = 16000000 * 1024,
            memoryPercent = 37.5f,
            cpuPercent = 25.5f,
            timestamp = System.currentTimeMillis()
        )
    }
    
    override suspend fun getMemoryPercent(): Float = 37.5f
    override suspend fun getCpuPercent(): Float = 25.5f
}

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state starts polling and fetches mem info`() =
        runTest {
            val viewModel = StatsViewModel(
                statsRepository = FakeStatsRepository(),
                context = android.app.Application()
            )
            assertTrue(viewModel.uiState.value.isPolling)
            // Memory calculation: (16000000 - 10000000) * 1024 = 6000000 * 1024
            assertEquals(6000000L * 1024, viewModel.uiState.value.memInfo.totalMemKb * 1024 - viewModel.uiState.value.memInfo.freeMemKb * 1024)
            viewModel.stopPolling()
        }

    @Test
    fun `toggle live overlay updates state`() =
        runTest {
            val viewModel = StatsViewModel(
                statsRepository = FakeStatsRepository(),
                context = android.app.Application()
            )
            viewModel.onAction(StatsUiAction.ToggleLiveOverlay(true))
            assertTrue(viewModel.uiState.value.isLiveOverlayActive)
            viewModel.stopPolling()
        }
}

package com.squidink.alloy.modules.statspill

import com.squidink.alloy.core.proc.MemInfo
import com.squidink.alloy.core.proc.ProcReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeProcReader : ProcReader() {
    override fun readMemInfo(): MemInfo {
        return MemInfo(totalMemKb = 16000000L, freeMemKb = 8000000L, availableMemKb = 10000000L)
    }

    override fun readCpuUsagePercent(): Float? = 25.5f
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
    fun `initial state starts polling and fetches mem info`() = runTest {
        val viewModel = StatsViewModel(FakeProcReader(), testDispatcher)
        assertTrue(viewModel.uiState.value.isPolling)
        assertEquals(16000000L, viewModel.uiState.value.memInfo.totalMemKb)
        assertEquals(25.5f, viewModel.uiState.value.cpuUsagePercent)
        viewModel.stopPolling()
    }

    @Test
    fun `toggle live overlay updates state`() = runTest {
        val viewModel = StatsViewModel(FakeProcReader(), testDispatcher)
        viewModel.onAction(StatsUiAction.ToggleLiveOverlay(true))
        assertTrue(viewModel.uiState.value.isLiveOverlayActive)
        viewModel.stopPolling()
    }
}

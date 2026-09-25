package com.squidink.alloy.modules.statspill

import android.content.Context
import com.squidink.alloy.core.domain.repository.IStatsRepository
import com.squidink.alloy.core.domain.repository.SystemStats
import com.squidink.alloy.core.permissions.AppPermission
import com.squidink.alloy.core.permissions.PermissionUiState
import com.squidink.alloy.core.permissions.PermissionsManager
import com.squidink.alloy.core.proc.NetStats
import com.squidink.alloy.core.proc.ProcReader
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

class MockProcReader : ProcReader() {
    override fun readNetworkStats(): NetStats {
        return NetStats(
            rxBytes = 1000L,
            txBytes = 500L,
            rxKbps = 0f,
            txKbps = 0f
        )
    }
}

// Simple test permissions manager that always grants permissions
class TestPermissionsManager : PermissionsManager() {
    override fun isPermissionGranted(context: Context, permission: AppPermission): Boolean {
        return true
    }
    
    override fun getPermissionState(context: Context, activity: android.app.Activity, permission: AppPermission): PermissionUiState {
        return PermissionUiState(
            permission = permission,
            state = com.squidink.alloy.core.permissions.PermissionState.Granted
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockContext: Context = android.app.Application()
    private val mockPermissionsManager: PermissionsManager = TestPermissionsManager()
    private val mockProcReader: ProcReader = MockProcReader()

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
                procReader = mockProcReader,
                context = mockContext,
                permissionsManager = mockPermissionsManager
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
                procReader = mockProcReader,
                context = mockContext,
                permissionsManager = mockPermissionsManager
            )
            viewModel.onAction(StatsUiAction.ToggleLiveOverlay(true))
            assertTrue(viewModel.uiState.value.isLiveOverlayActive)
            viewModel.stopPolling()
        }
}

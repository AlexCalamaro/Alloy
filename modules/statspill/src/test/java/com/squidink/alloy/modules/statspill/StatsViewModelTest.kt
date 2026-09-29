package com.squidink.alloy.modules.statspill

import app.cash.turbine.test
import com.squidink.alloy.core.data.repository.SettingsRepository
import com.squidink.alloy.modules.statspill.domain.model.CombinedTelemetry
import com.squidink.alloy.modules.statspill.domain.model.ErrorType
import com.squidink.alloy.modules.statspill.domain.model.StatCategory
import com.squidink.alloy.modules.statspill.domain.model.StatError
import com.squidink.alloy.modules.statspill.domain.model.SystemStats
import com.squidink.alloy.modules.statspill.domain.usecase.ObserveErrorsUseCase
import com.squidink.alloy.modules.statspill.domain.usecase.ObserveTelemetryUseCase
import com.squidink.alloy.modules.statspill.domain.usecase.PollTelemetryUseCase
import com.squidink.alloy.modules.statspill.stats.OverlayServiceManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val telemetryFlow = MutableSharedFlow<CombinedTelemetry>(replay = 1)
    private val errorFlow = MutableSharedFlow<StatError>(replay = 1)
    private val usePercentagesFlow = MutableSharedFlow<Boolean>(replay = 1)
    private val cornerPositionFlow = MutableSharedFlow<String>(replay = 1)

    private val observeTelemetryUseCase: ObserveTelemetryUseCase = mockk()
    private val pollTelemetryUseCase: PollTelemetryUseCase = mockk()
    private val observeErrorsUseCase: ObserveErrorsUseCase = mockk()
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val overlayManager: OverlayServiceManager = mockk(relaxed = true)

    private val testTelemetry = CombinedTelemetry(
        systemStats = SystemStats(
            timestamp = 1000L,
            memoryUsedBytes = 4000L,
            memoryTotalBytes = 8000L,
            memoryPercent = 50.0f,
            cpuPercent = 25.0f
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { observeTelemetryUseCase() } returns telemetryFlow
        every { observeErrorsUseCase() } returns errorFlow
        every { settingsRepository.observeUsePercentages() } returns usePercentagesFlow
        every { settingsRepository.observeCornerPosition() } returns cornerPositionFlow
        every { overlayManager.isPermissionGranted() } returns true
        coEvery { pollTelemetryUseCase() } returns testTelemetry

        telemetryFlow.tryEmit(testTelemetry)
        usePercentagesFlow.tryEmit(true)
        cornerPositionFlow.tryEmit("TOP_RIGHT")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): StatsViewModel {
        return StatsViewModel(
            observeTelemetryUseCase = observeTelemetryUseCase,
            pollTelemetryUseCase = pollTelemetryUseCase,
            observeErrorsUseCase = observeErrorsUseCase,
            settingsRepository = settingsRepository,
            overlayManager = overlayManager
        )
    }

    @Test
    fun `initialization observes telemetry and sets initial state`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(50.0f, state.systemStats?.memoryPercent)
        assertEquals(25.0f, state.systemStats?.cpuPercent)
        assertTrue(state.usePercentages)
        assertEquals(CornerPosition.TOP_RIGHT, state.cornerPosition)
        assertTrue(state.isLiveOverlayPermissionGranted)
    }

    @Test
    fun `TogglePolling action toggles isPolling state`() = runTest {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isPolling)

        viewModel.onAction(StatsUiAction.TogglePolling)
        assertFalse(viewModel.uiState.value.isPolling)

        viewModel.onAction(StatsUiAction.TogglePolling)
        assertTrue(viewModel.uiState.value.isPolling)
    }

    @Test
    fun `RefreshNow updates telemetry state`() = runTest {
        val viewModel = createViewModel()

        val updatedTelemetry = CombinedTelemetry(
            systemStats = SystemStats(
                timestamp = 2000L,
                memoryUsedBytes = 6000L,
                memoryTotalBytes = 8000L,
                memoryPercent = 75.0f,
                cpuPercent = 80.0f
            )
        )
        coEvery { pollTelemetryUseCase() } returns updatedTelemetry

        viewModel.onAction(StatsUiAction.RefreshNow)

        coVerify { pollTelemetryUseCase() }
        assertEquals(75.0f, viewModel.uiState.value.systemStats?.memoryPercent)
        assertEquals(80.0f, viewModel.uiState.value.systemStats?.cpuPercent)
    }

    @Test
    fun `ToggleLiveOverlay with permission granted starts overlay`() = runTest {
        every { overlayManager.isPermissionGranted() } returns true
        every { overlayManager.startOverlay() } returns true

        val viewModel = createViewModel()
        viewModel.onAction(StatsUiAction.ToggleLiveOverlay(true))

        assertTrue(viewModel.uiState.value.isLiveOverlayActive)
        verify { overlayManager.startOverlay() }
    }

    @Test
    fun `ToggleLiveOverlay with permission denied emits OpenOverlayPermissionSettings effect`() = runTest {
        every { overlayManager.isPermissionGranted() } returns false

        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.onAction(StatsUiAction.ToggleLiveOverlay(true))

            assertFalse(viewModel.uiState.value.isLiveOverlayActive)
            assertFalse(viewModel.uiState.value.isLiveOverlayPermissionGranted)
            val effect = awaitItem()
            assertTrue(effect is StatsUiEffect.OpenOverlayPermissionSettings)
        }
    }

    @Test
    fun `ToggleLiveOverlay false stops overlay`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAction(StatsUiAction.ToggleLiveOverlay(false))

        assertFalse(viewModel.uiState.value.isLiveOverlayActive)
        verify { overlayManager.stopOverlay() }
    }

    @Test
    fun `UpdateUsePercentages updates state and persists`() = runTest {
        val viewModel = createViewModel()

        viewModel.onAction(StatsUiAction.UpdateUsePercentages(false))

        assertFalse(viewModel.uiState.value.usePercentages)
        coVerify { settingsRepository.setUsePercentages(false) }
    }

    @Test
    fun `UpdateCornerPosition updates state and persists`() = runTest {
        val viewModel = createViewModel()

        viewModel.onAction(StatsUiAction.UpdateCornerPosition(CornerPosition.BOTTOM_LEFT))

        assertEquals(CornerPosition.BOTTOM_LEFT, viewModel.uiState.value.cornerPosition)
        coVerify { settingsRepository.setCornerPosition("BOTTOM_LEFT") }
    }

    @Test
    fun `observeErrors updates error map and ClearError removes it`() = runTest {
        val viewModel = createViewModel()

        val error = StatError(
            category = StatCategory.SYSTEM,
            type = ErrorType.READ_ERROR,
            message = "Test error"
        )
        errorFlow.tryEmit(error)

        assertNotNull(viewModel.uiState.value.errors[StatCategory.SYSTEM])
        assertEquals("Test error", viewModel.uiState.value.errors[StatCategory.SYSTEM]?.message)

        viewModel.onAction(StatsUiAction.ClearError(StatCategory.SYSTEM))
        assertFalse(viewModel.uiState.value.errors.containsKey(StatCategory.SYSTEM))
    }
}

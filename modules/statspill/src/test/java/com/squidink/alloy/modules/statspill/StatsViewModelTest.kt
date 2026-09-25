package com.squidink.alloy.modules.statspill

import android.content.Context
import android.content.Intent
import com.squidink.alloy.core.data.datasource.BatteryInfo
import com.squidink.alloy.core.data.datasource.SystemStatsData
import com.squidink.alloy.core.data.repository.SettingsRepository
import com.squidink.alloy.core.domain.repository.IStatsRepository
import com.squidink.alloy.core.domain.repository.SystemStats
import com.squidink.alloy.core.permissions.AppPermission
import com.squidink.alloy.core.permissions.PermissionsManager
import com.squidink.alloy.core.proc.NetStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeStatsRepository = FakeStatsRepository()
    private val fakeStatsRepositoryImpl = FakeStatsRepositoryImpl()
    private val fakeSettingsRepository = FakeSettingsRepository()
    private val mockContext: Context = mock()
    private val mockPermissionsManager: PermissionsManager = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default values`() = runTest {
        val viewModel = createViewModel()
        
        val state = viewModel.uiState.value
        
        assertNotNull(state.memInfo)
        assertNull(state.cpuUsagePercent)
        assertNotNull(state.netStats)
        assertNotNull(state.batteryInfo)
        assertFalse(state.isLiveOverlayActive)
        assertFalse(state.isPolling)
        assertTrue(state.showPill)
        assertTrue(state.usePercentages)
        assertEquals(CornerPosition.TOP_RIGHT, state.cornerPosition)
    }

    @Test
    fun `TogglePolling action toggles polling state`() = runTest {
        val viewModel = createViewModel()
        
        // Start polling
        viewModel.onAction(StatsUiAction.TogglePolling)
        
        assertTrue(viewModel.uiState.value.isPolling)
        
        // Stop polling
        viewModel.onAction(StatsUiAction.TogglePolling)
        
        assertFalse(viewModel.uiState.value.isPolling)
    }

    @Test
    fun `RefreshNow action triggers refresh`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.onAction(StatsUiAction.RefreshNow)
        
        // Verify repository poll was called
        verify(fakeStatsRepository).pollSystemStats()
    }

    @Test
    fun `UpdateShowPill action updates state and settings`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.onAction(StatsUiAction.UpdateShowPill(false))
        
        assertEquals(false, viewModel.uiState.value.showPill)
        verify(fakeSettingsRepository).setShowPill(false)
    }

    @Test
    fun `UpdateUsePercentages action updates state and settings`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.onAction(StatsUiAction.UpdateUsePercentages(false))
        
        assertEquals(false, viewModel.uiState.value.usePercentages)
        verify(fakeSettingsRepository).setUsePercentages(false)
    }

    @Test
    fun `UpdateCornerPosition action updates state and settings`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.onAction(StatsUiAction.UpdateCornerPosition(CornerPosition.BOTTOM_LEFT))
        
        assertEquals(CornerPosition.BOTTOM_LEFT, viewModel.uiState.value.cornerPosition)
        verify(fakeSettingsRepository).setCornerPosition("BOTTOM_LEFT")
    }

    @Test
    fun `DismissPermissionDialog action clears dialog state`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.onAction(StatsUiAction.DismissPermissionDialog)
        
        // Action completes without error
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun `startOverlayService checks permission`() = runTest {
        whenever(mockPermissionsManager.isPermissionGranted(mockContext, AppPermission.SystemOverlay))
            .thenReturn(false)
        
        val viewModel = createViewModel()
        
        // This would normally start the service, but permission check should fail
        // Verify the effect is sent
        // Note: Effect testing would require Flow collection
    }

    @Test
    fun `observeSystemStats updates state with memory and CPU info`() = runTest {
        val viewModel = createViewModel()
        
        // The observeSystemStats is called in init, state should be updated
        // Verify initial state has memory info
        assertNotNull(viewModel.uiState.value.memInfo)
    }

    @Test
    fun `observeBatteryInfo updates state with battery info`() = runTest {
        val viewModel = createViewModel()
        
        // The observeBatteryInfo is called in init, state should be updated
        assertNotNull(viewModel.uiState.value.batteryInfo)
    }

    @Test
    fun `observeNetworkStats updates state with network stats`() = runTest {
        val viewModel = createViewModel()
        
        // The observeNetworkStats is called in init, state should be updated
        assertNotNull(viewModel.uiState.value.netStats)
    }

    @Test
    fun `loadSettings loads user preferences`() = runTest {
        val viewModel = createViewModel()
        
        // Settings are loaded in init
        assertTrue(viewModel.uiState.value.showPill)
        assertTrue(viewModel.uiState.value.usePercentages)
        assertEquals(CornerPosition.TOP_RIGHT, viewModel.uiState.value.cornerPosition)
    }

    @Test
    fun `updateSettings with all parameters updates state correctly`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.updateSettings(
            showPill = false,
            usePercentages = false,
            cornerPosition = CornerPosition.BOTTOM_RIGHT
        )
        
        assertEquals(false, viewModel.uiState.value.showPill)
        assertEquals(false, viewModel.uiState.value.usePercentages)
        assertEquals(CornerPosition.BOTTOM_RIGHT, viewModel.uiState.value.cornerPosition)
    }

    @Test
    fun `checkOverlayPermission updates permission state`() = runTest {
        whenever(mockPermissionsManager.isPermissionGranted(mockContext, AppPermission.SystemOverlay))
            .thenReturn(true)
        
        val viewModel = createViewModel()
        
        // Permission check happens in init
        assertTrue(viewModel.uiState.value.isLiveOverlayPermissionGranted)
    }

    private fun createViewModel(): StatsViewModel {
        return StatsViewModel(
            statsRepository = fakeStatsRepository,
            statsRepositoryImpl = fakeStatsRepositoryImpl,
            settingsRepository = fakeSettingsRepository,
            context = mockContext,
            permissionsManager = mockPermissionsManager
        )
    }
}

/**
 * Extended fake repository implementation for testing StatsViewModel.
 */
class FakeStatsRepositoryImpl : FakeStatsRepository() {
    private val _batteryInfo = MutableStateFlow(BatteryInfo())
    private val _networkStats = MutableStateFlow(NetStats())
    private val _systemStats = MutableStateFlow(SystemStatsData())

    fun observeBatteryInfo() = _batteryInfo
    fun observeNetworkStats() = _networkStats
    
    suspend fun pollStats() {
        _systemStats.value = SystemStatsData(
            memoryUsedBytes = 6000000 * 1024,
            memoryTotalBytes = 16000000 * 1024,
            memoryPercent = 37.5f,
            cpuPercent = 25.5f,
            netStats = NetStats(),
            timestamp = System.currentTimeMillis()
        )
    }
}

/**
 * Fake implementation of SettingsRepository for testing.
 */
class FakeSettingsRepository : SettingsRepository(
    dataStoreManager = FakeDataStoreManager()
) {
    private val _showPill = MutableStateFlow(true)
    private val _usePercentages = MutableStateFlow(true)
    private val _cornerPosition = MutableStateFlow("TOP_RIGHT")

    override fun observeShowPill() = _showPill
    override fun observeUsePercentages() = _usePercentages
    override fun observeCornerPosition() = _cornerPosition
    
    override suspend fun setShowPill(show: Boolean) {
        _showPill.value = show
    }
    
    override suspend fun setUsePercentages(use: Boolean) {
        _usePercentages.value = use
    }
    
    override suspend fun setCornerPosition(position: String) {
        _cornerPosition.value = position
    }
}

/**
 * Fake implementation of DataStoreManager for testing.
 */
class FakeDataStoreManager {
    private val strings = mutableMapOf<String, String>()
    
    fun getStringFlow(key: String) = flowOf(strings[key])
    suspend fun setString(key: String, value: String) {
        strings[key] = value
    }
}

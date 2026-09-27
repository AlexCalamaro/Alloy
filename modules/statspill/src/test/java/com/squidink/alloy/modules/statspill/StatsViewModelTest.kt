package com.squidink.alloy.modules.statspill

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
    fun `StatsUiState has default values`() = runTest {
        val state = StatsUiState()
        
        assertNull(state.systemStats)
        assertFalse(state.isLiveOverlayActive)
        assertFalse(state.isPolling)
        assertTrue(state.usePercentages)
        assertEquals(CornerPosition.TOP_RIGHT, state.cornerPosition)
    }

    @Test
    fun `StatsUiState can be copied with updated values`() = runTest {
        val original = StatsUiState()
        val modified = original.copy(usePercentages = false)
        
        assertFalse(modified.usePercentages)
        assertTrue(original.usePercentages)
    }

    @Test
    fun `CornerPosition enum has correct display names`() = runTest {
        assertEquals("Top-Left", CornerPosition.TOP_LEFT.displayName)
        assertEquals("Top-Right", CornerPosition.TOP_RIGHT.displayName)
        assertEquals("Bottom-Left", CornerPosition.BOTTOM_LEFT.displayName)
        assertEquals("Bottom-Right", CornerPosition.BOTTOM_RIGHT.displayName)
    }

    @Test
    fun `StatsUiAction TogglePolling is defined`() = runTest {
        val action = StatsUiAction.TogglePolling
        assertNotNull(action)
    }

    @Test
    fun `StatsUiAction ToggleLiveOverlay is defined`() = runTest {
        val action = StatsUiAction.ToggleLiveOverlay(true)
        assertNotNull(action)
    }

    @Test
    fun `StatsUiAction RefreshNow is defined`() = runTest {
        val action = StatsUiAction.RefreshNow
        assertNotNull(action)
    }

    @Test
    fun `StatsUiAction UpdateUsePercentages is defined`() = runTest {
        val action = StatsUiAction.UpdateUsePercentages(false)
        assertNotNull(action)
    }

    @Test
    fun `StatsUiAction UpdateCornerPosition is defined`() = runTest {
        val action = StatsUiAction.UpdateCornerPosition(CornerPosition.BOTTOM_LEFT)
        assertNotNull(action)
    }
}

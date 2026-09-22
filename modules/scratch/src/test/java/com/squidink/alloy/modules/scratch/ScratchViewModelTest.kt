package com.squidink.alloy.modules.scratch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScratchViewModelTest {

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
    fun `update content updates state`() = runTest {
        val viewModel = ScratchViewModel()
        viewModel.onAction(ScratchUiAction.UpdateContent("New note content"))
        assertEquals("New note content", viewModel.uiState.value.noteContent)
    }

    @Test
    fun `toggle timer starts and stops timer`() = runTest {
        val viewModel = ScratchViewModel()
        assertFalse(viewModel.uiState.value.isTimerRunning)

        viewModel.onAction(ScratchUiAction.ToggleTimer)
        assertTrue(viewModel.uiState.value.isTimerRunning)

        viewModel.onAction(ScratchUiAction.ToggleTimer)
        assertFalse(viewModel.uiState.value.isTimerRunning)
        viewModel.stopTimer()
    }
}

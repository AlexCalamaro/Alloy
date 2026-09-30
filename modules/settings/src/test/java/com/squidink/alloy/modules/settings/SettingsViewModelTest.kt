package com.squidink.alloy.modules.settings

import com.squidink.alloy.core.data.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { settingsRepository.observeDynamicColor() } returns flowOf(true)
        every { settingsRepository.observeUsePercentages() } returns flowOf(true)
        every { settingsRepository.observeCacheEnabled() } returns flowOf(true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        val viewModel = SettingsViewModel(settingsRepository)
        assertEquals(
            SettingsUiState(dynamicColor = true, usePercentages = true, cacheEnabled = true),
            viewModel.uiState.value
        )
    }

    @Test
    fun `action SetDynamicColor updates state`() = runTest {
        val viewModel = SettingsViewModel(settingsRepository)
        viewModel.onAction(SettingsUiAction.SetDynamicColor(false))
        assertEquals(false, viewModel.uiState.value.dynamicColor)
    }
}

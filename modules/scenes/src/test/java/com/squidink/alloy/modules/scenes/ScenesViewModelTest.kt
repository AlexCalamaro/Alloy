package com.squidink.alloy.modules.scenes

import android.content.ContextWrapper
import com.squidink.alloy.modules.scenes.model.PlacementHint
import com.squidink.alloy.modules.scenes.model.Scene
import com.squidink.alloy.modules.scenes.model.SceneStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScenesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val launcher = SceneLauncher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `calculateBounds returns correct Rect for PlacementHint`() {
        val leftBounds = launcher.calculateBounds(PlacementHint.LEFT, 1920, 1080)
        assertEquals(0, leftBounds.left)
        assertEquals(0, leftBounds.top)
        assertEquals(960, leftBounds.right)
        assertEquals(1080, leftBounds.bottom)

        val rightBounds = launcher.calculateBounds(PlacementHint.RIGHT, 1920, 1080)
        assertEquals(960, rightBounds.left)
        assertEquals(1920, rightBounds.right)
    }

    @Test
    fun `add and delete scene updates state`() = runTest {
        val viewModel = ScenesViewModel(launcher, ContextWrapper(null))
        val initialCount = viewModel.uiState.value.scenes.size

        val newScene = Scene("test_2", "Design Stack", steps = listOf(SceneStep("1", "com.example.app")))
        viewModel.onAction(ScenesUiAction.AddScene(newScene))
        assertEquals(initialCount + 1, viewModel.uiState.value.scenes.size)

        viewModel.onAction(ScenesUiAction.DeleteScene("test_2"))
        assertEquals(initialCount, viewModel.uiState.value.scenes.size)
    }
}

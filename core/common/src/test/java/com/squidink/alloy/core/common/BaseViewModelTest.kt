package com.squidink.alloy.core.common

import app.cash.turbine.test
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

data class TestState(val count: Int = 0) : UiState
sealed interface TestAction : UiAction {
    data object Increment : TestAction
    data object TriggerEffect : TestAction
}
sealed interface TestEffect : UiEffect {
    data object Toast : TestEffect
}

class TestViewModel : BaseViewModel<TestState, TestAction, TestEffect>(TestState()) {
    override fun onAction(action: TestAction) {
        when (action) {
            TestAction.Increment -> updateState { it.copy(count = it.count + 1) }
            TestAction.TriggerEffect -> sendEffect(TestEffect.Toast)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {

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
    fun `state updates correctly when action dispatched`() = runTest {
        val viewModel = TestViewModel()
        assertEquals(0, viewModel.uiState.value.count)

        viewModel.onAction(TestAction.Increment)
        assertEquals(1, viewModel.uiState.value.count)
    }

    @Test
    fun `effect emitted when action dispatched`() = runTest {
        val viewModel = TestViewModel()

        viewModel.effect.test {
            viewModel.onAction(TestAction.TriggerEffect)
            assertEquals(TestEffect.Toast, awaitItem())
        }
    }
}

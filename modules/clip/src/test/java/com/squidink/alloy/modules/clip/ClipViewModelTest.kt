package com.squidink.alloy.modules.clip

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

@OptIn(ExperimentalCoroutinesApi::class)
class ClipViewModelTest {

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
    fun `text transformations execute correctly`() {
        assertEquals("HELLO WORLD", ClipTransformations.toUpperCase("hello world"))
        assertEquals("hello world", ClipTransformations.toLowerCase("HELLO WORLD"))
        assertEquals("trimmed", ClipTransformations.trimWhitespace("  trimmed  "))
    }

    @Test
    fun `add clip and search filter updates state`() = runTest {
        val viewModel = ClipViewModel()
        val initialCount = viewModel.uiState.value.clips.size

        viewModel.onAction(ClipUiAction.AddClip("UniqueSearchableText"))
        assertEquals(initialCount + 1, viewModel.uiState.value.clips.size)

        viewModel.onAction(ClipUiAction.UpdateSearchQuery("Unique"))
        assertEquals("Unique", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `apply transformation emits CopyToClipboard effect`() = runTest {
        val viewModel = ClipViewModel()
        val firstClip = viewModel.uiState.value.clips.first()
        viewModel.onAction(ClipUiAction.SelectClip(firstClip))

        viewModel.effect.test {
            viewModel.onAction(ClipUiAction.ApplyTransformation(TransformationType.UPPERCASE))
            val effect = awaitItem() as ClipUiEffect.CopyToClipboard
            assertEquals(firstClip.textContent.uppercase(), effect.text)
        }
    }
}

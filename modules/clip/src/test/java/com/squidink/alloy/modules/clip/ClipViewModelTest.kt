package com.squidink.alloy.modules.clip

import app.cash.turbine.test
import com.squidink.alloy.modules.clip.db.ClipDao
import com.squidink.alloy.modules.clip.db.ClipEntity
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
import org.junit.Before
import org.junit.Test

class FakeClipDao : ClipDao {
    private val testClips =
        listOf(
            ClipEntity("1", "https://github.com/squidink/alloy", sourceApp = "Chrome", isPinned = true),
            ClipEntity("2", "val apiKey = \"secret_12345\"", sourceApp = "DeskTerm", isPinned = false),
        )

    override fun getAllClips(): Flow<List<ClipEntity>> = flowOf(testClips)

    override fun searchClips(query: String): Flow<List<ClipEntity>> = flowOf(testClips)

    override suspend fun insertClip(clip: ClipEntity) {}

    override suspend fun updateClip(
        id: String,
        textContent: String,
        isPinned: Boolean,
    ) {}

    override suspend fun deleteClip(id: String) {}

    override suspend fun clearUnpinnedClips() {}
}

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
    fun `add clip and search filter updates state`() =
        runTest {
            val viewModel = ClipViewModel(FakeClipDao(), enableCleanupTask = false)
            val initialCount = viewModel.uiState.value.clips.size

            viewModel.onAction(ClipUiAction.AddClip("UniqueSearchableText"))
            assertEquals(initialCount + 1, viewModel.uiState.value.clips.size)

            viewModel.onAction(ClipUiAction.UpdateSearchQuery("Unique"))
            assertEquals("Unique", viewModel.uiState.value.searchQuery)
        }

    @Test
    fun `apply transformation emits CopyToClipboard effect`() =
        runTest {
            val viewModel = ClipViewModel(FakeClipDao(), enableCleanupTask = false)
            val firstClip =
                viewModel.uiState.value.clips
                    .first()
            viewModel.onAction(ClipUiAction.SelectClip(firstClip))

            viewModel.effect.test {
                viewModel.onAction(ClipUiAction.ApplyTransformation(TransformationType.UPPER_CASE))
                val effect = awaitItem() as ClipUiEffect.CopyToClipboard
                assertEquals(firstClip.textContent.uppercase(), effect.text)
            }
        }
}

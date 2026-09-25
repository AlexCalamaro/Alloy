package com.squidink.alloy.modules.clip

import android.content.Context
import app.cash.turbine.test
import com.squidink.alloy.core.domain.repository.Clip
import com.squidink.alloy.core.domain.repository.IClipRepository
import com.squidink.alloy.core.permissions.AppPermission
import com.squidink.alloy.core.permissions.PermissionUiState
import com.squidink.alloy.core.permissions.PermissionsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FakeClipRepository : IClipRepository {
    private val testClips = listOf(
        Clip(
            id = "1",
            textContent = "https://github.com/squidink/alloy",
            sourceApp = "Chrome",
            isPinned = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ),
        Clip(
            id = "2",
            textContent = "val apiKey = \"secret_12345\"",
            sourceApp = "DeskTerm",
            isPinned = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ),
    )

    override fun getClips(): Flow<List<Clip>> = flowOf(testClips)

    override fun getClipById(id: String): Flow<Clip?> = flowOf(null)

    override suspend fun insertClip(clip: Clip) {}

    override suspend fun updateClip(clip: Clip) {}

    override suspend fun deleteClip(id: String) {}

    override suspend fun pinClip(id: String) {}

    override suspend fun unpinClip(id: String) {}

    override suspend fun deleteAllClips() {}
}

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
class ClipViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockContext: Context = android.app.Application()
    private val mockPermissionsManager: PermissionsManager = TestPermissionsManager()

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
            val viewModel = ClipViewModel(FakeClipRepository(), mockPermissionsManager, mockContext)
            
            // Update search query
            viewModel.onAction(ClipUiAction.UpdateSearchQuery("Unique"))
            assertEquals("Unique", viewModel.uiState.value.searchQuery)
        }

    @Test
    fun `apply transformation emits CopyToClipboard effect`() =
        runTest {
            val viewModel = ClipViewModel(FakeClipRepository(), mockPermissionsManager, mockContext)
            // Select the first clip from the initial state (GitHub URL)
            val githubClip = viewModel.uiState.value.clips.first { it.id == "1" }
            
            viewModel.effect.test {
                viewModel.onAction(ClipUiAction.SelectClip(githubClip))
                // Consume the CopyToClipboard effect from SelectClip
                awaitItem()
                
                viewModel.onAction(ClipUiAction.ApplyTransformation(TransformationType.UPPER_CASE))
                val effect = awaitItem() as ClipUiEffect.CopyToClipboard
                assertEquals(githubClip.textContent.uppercase(), effect.text)
                // Consume the ShowToast effect
                awaitItem()
            }
        }
}

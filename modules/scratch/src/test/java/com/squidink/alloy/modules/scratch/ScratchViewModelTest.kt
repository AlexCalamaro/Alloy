package com.squidink.alloy.modules.scratch

import com.squidink.alloy.core.domain.common.repository.IScratchRepository
import com.squidink.alloy.core.domain.common.repository.Scratch
import com.squidink.alloy.modules.scratch.model.EditorLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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

class FakeScratchRepository(
    initialDocs: List<Scratch> = emptyList()
) : IScratchRepository {
    private val docsFlow = MutableStateFlow(initialDocs)

    override fun getScratchpads(): Flow<List<Scratch>> = docsFlow

    override fun getScratchpadById(id: String): Flow<Scratch?> =
        docsFlow.map { docs -> docs.find { it.id == id } }

    override suspend fun insertScratchpad(scratch: Scratch) {
        docsFlow.value = docsFlow.value + scratch
    }

    override suspend fun updateScratchpad(scratch: Scratch) {
        docsFlow.value = docsFlow.value.map { if (it.id == scratch.id) scratch else it }
    }

    override suspend fun deleteScratchpad(id: String) {
        docsFlow.value = docsFlow.value.filter { it.id != id }
    }

    override suspend fun deleteAllScratchpads() {
        docsFlow.value = emptyList()
    }
}

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
    fun `initialization creates default document if repository is empty`() = runTest {
        val repo = FakeScratchRepository()
        val viewModel = ScratchViewModel(repo)

        val state = viewModel.uiState.value
        assertEquals(1, state.documents.size)
        assertEquals("default_scratch_note", state.activeDocumentId)
        assertNotNull(state.activeDocument)
        assertFalse(state.isCurrentDocumentLocked)
    }

    @Test
    fun `update content updates active document state`() = runTest {
        val testDoc = Scratch(id = "doc1", title = "Test", content = "Old content")
        val repo = FakeScratchRepository(listOf(testDoc))
        val viewModel = ScratchViewModel(repo)

        viewModel.onAction(ScratchUiAction.UpdateContent("Updated content"))
        assertEquals("Updated content", viewModel.uiState.value.activeDocument?.content)
    }

    @Test
    fun `create new document via FAB adds tab and selects it`() = runTest {
        val testDoc = Scratch(id = "doc1", title = "Doc 1", content = "Content 1")
        val repo = FakeScratchRepository(listOf(testDoc))
        val viewModel = ScratchViewModel(repo)

        viewModel.onAction(ScratchUiAction.CreateNewDocument)

        val state = viewModel.uiState.value
        assertEquals(2, state.documents.size)
        val newDoc = state.documents.last()
        assertEquals(newDoc.id, state.activeDocumentId)
        assertEquals("Untitled 2", newDoc.title)
    }

    @Test
    fun `select document switches active tab`() = runTest {
        val doc1 = Scratch(id = "doc1", title = "Doc 1")
        val doc2 = Scratch(id = "doc2", title = "Doc 2")
        val repo = FakeScratchRepository(listOf(doc1, doc2))
        val viewModel = ScratchViewModel(repo)

        viewModel.onAction(ScratchUiAction.SelectDocument("doc2"))
        assertEquals("doc2", viewModel.uiState.value.activeDocumentId)
    }

    @Test
    fun `close document deletes document and selects remaining tab`() = runTest {
        val doc1 = Scratch(id = "doc1", title = "Doc 1")
        val doc2 = Scratch(id = "doc2", title = "Doc 2")
        val repo = FakeScratchRepository(listOf(doc1, doc2))
        val viewModel = ScratchViewModel(repo)

        viewModel.onAction(ScratchUiAction.SelectDocument("doc1"))
        viewModel.onAction(ScratchUiAction.CloseDocument("doc1"))

        val state = viewModel.uiState.value
        assertEquals(1, state.documents.size)
        assertEquals("doc2", state.activeDocumentId)
    }

    @Test
    fun `rename document updates document title`() = runTest {
        val doc = Scratch(id = "doc1", title = "Old Title")
        val repo = FakeScratchRepository(listOf(doc))
        val viewModel = ScratchViewModel(repo)

        viewModel.onAction(ScratchUiAction.RenameDocument("doc1", "New Title.kt"))
        assertEquals("New Title.kt", viewModel.uiState.value.activeDocument?.title)
    }

    @Test
    fun `set document language updates language in state`() = runTest {
        val doc = Scratch(id = "doc1", title = "Doc 1", language = "PLAIN_TEXT")
        val repo = FakeScratchRepository(listOf(doc))
        val viewModel = ScratchViewModel(repo)

        viewModel.onAction(ScratchUiAction.SetDocumentLanguage("doc1", EditorLanguage.KOTLIN))
        assertEquals(EditorLanguage.KOTLIN.name, viewModel.uiState.value.activeDocument?.language)
    }

    @Test
    fun `lock document requires confirmation and marks document locked`() = runTest {
        val doc = Scratch(id = "doc1", title = "Secrets", isLocked = false)
        val repo = FakeScratchRepository(listOf(doc))
        val viewModel = ScratchViewModel(repo)

        // Requesting lock shows confirmation dialog
        viewModel.onAction(ScratchUiAction.RequestToggleLock("doc1"))
        assertTrue(viewModel.uiState.value.showLockConfirmationDialog)
        assertEquals("doc1", viewModel.uiState.value.pendingLockDocumentId)

        // Confirming lock sets isLocked = true
        viewModel.onAction(ScratchUiAction.ConfirmLockDocument("doc1"))
        assertFalse(viewModel.uiState.value.showLockConfirmationDialog)
        assertTrue(viewModel.uiState.value.activeDocument?.isLocked == true)
        assertTrue(viewModel.uiState.value.isCurrentDocumentLocked)
    }

    @Test
    fun `authenticate document success unlocks document for active session`() = runTest {
        val doc = Scratch(id = "doc1", title = "Secrets", isLocked = true)
        val repo = FakeScratchRepository(listOf(doc))
        val viewModel = ScratchViewModel(repo)

        assertTrue(viewModel.uiState.value.isCurrentDocumentLocked)

        viewModel.onAction(ScratchUiAction.AuthenticateDocumentSuccess("doc1"))
        assertFalse(viewModel.uiState.value.isCurrentDocumentLocked)
        assertTrue(viewModel.uiState.value.unlockedDocumentIds.contains("doc1"))
    }

    @Test
    fun `lock all documents immediately clears unlocked session IDs`() = runTest {
        val doc = Scratch(id = "doc1", title = "Secrets", isLocked = true)
        val repo = FakeScratchRepository(listOf(doc))
        val viewModel = ScratchViewModel(repo)

        viewModel.onAction(ScratchUiAction.AuthenticateDocumentSuccess("doc1"))
        assertFalse(viewModel.uiState.value.isCurrentDocumentLocked)

        viewModel.onAction(ScratchUiAction.LockAllDocuments)
        assertTrue(viewModel.uiState.value.isCurrentDocumentLocked)
        assertTrue(viewModel.uiState.value.unlockedDocumentIds.isEmpty())
    }


    @Test
    fun `request export on unlocked document emits LaunchExportPicker effect`() = runTest {
        val doc = Scratch(id = "doc1", title = "MyScript", content = "println(1)", language = "KOTLIN", isLocked = false)
        val repo = FakeScratchRepository(listOf(doc))
        val viewModel = ScratchViewModel(repo)

        val effects = mutableListOf<ScratchUiEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onAction(ScratchUiAction.RequestExportDocument("doc1"))
        testScheduler.runCurrent()

        assertEquals(1, effects.size)
        val effect = effects.first() as ScratchUiEffect.LaunchExportPicker
        assertEquals("doc1", effect.documentId)
        assertEquals("MyScript.kt", effect.fileName)
        assertEquals("text/x-kotlin", effect.mimeType)
        assertEquals("println(1)", effect.content)

        job.cancel()
    }

    @Test
    fun `request export on locked document requires biometric auth first`() = runTest {
        val doc = Scratch(id = "doc1", title = "SecretDoc", content = "Top Secret", language = "MARKDOWN", isLocked = true)
        val repo = FakeScratchRepository(listOf(doc))
        val viewModel = ScratchViewModel(repo)

        val effects = mutableListOf<ScratchUiEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onAction(ScratchUiAction.RequestExportDocument("doc1"))
        testScheduler.runCurrent()

        assertEquals(1, effects.size)
        val authEffect = effects.first() as ScratchUiEffect.RequestBiometricAuth
        assertEquals("doc1", authEffect.documentId)

        // Now authenticate successfully
        viewModel.onAction(ScratchUiAction.AuthenticateDocumentSuccess("doc1"))
        testScheduler.runCurrent()

        assertEquals(2, effects.size)
        val exportEffect = effects[1] as ScratchUiEffect.LaunchExportPicker
        assertEquals("SecretDoc.md", exportEffect.fileName)
        assertEquals("text/markdown", exportEffect.mimeType)
        assertEquals("Top Secret", exportEffect.content)

        job.cancel()
    }

    @Test
    fun `set user message updates state`() = runTest {
        val viewModel = ScratchViewModel(FakeScratchRepository())
        viewModel.onAction(ScratchUiAction.SetUserMessage("Exported successfully"))
        assertEquals("Exported successfully", viewModel.uiState.value.userMessage)
    }
}

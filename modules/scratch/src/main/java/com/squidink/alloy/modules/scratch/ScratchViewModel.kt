package com.squidink.alloy.modules.scratch

import androidx.lifecycle.viewModelScope
import com.squidink.alloy.core.common.BaseViewModel
import com.squidink.alloy.core.common.UiAction
import com.squidink.alloy.core.common.UiEffect
import com.squidink.alloy.core.common.UiState
import com.squidink.alloy.core.domain.common.repository.IScratchRepository
import com.squidink.alloy.core.domain.common.repository.Scratch
import com.squidink.alloy.modules.scratch.model.EditorLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChecklistItem(
    val id: String,
    val text: String,
    val isCompleted: Boolean = false,
)

data class ScratchUiState(
    val documents: List<Scratch> = emptyList(),
    val activeDocumentId: String? = null,
    val unlockedDocumentIds: Set<String> = emptySet(),
    val showLockConfirmationDialog: Boolean = false,
    val pendingLockDocumentId: String? = null,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val activePane: ScratchPane = ScratchPane.TEXT,
    val isTimerRunning: Boolean = false,
    val timerSeconds: Int = 0,
    val autoSaveDebounceMs: Long = 500,
    val userMessage: String? = null,
) : UiState {
    val activeDocument: Scratch?
        get() = documents.find { it.id == activeDocumentId } ?: documents.firstOrNull()

    val isCurrentDocumentLocked: Boolean
        get() {
            val doc = activeDocument ?: return false
            return doc.isLocked && !unlockedDocumentIds.contains(doc.id)
        }
}

sealed interface ScratchUiAction : UiAction {
    // Document tab actions
    data object CreateNewDocument : ScratchUiAction
    data class SelectDocument(val documentId: String) : ScratchUiAction
    data class CloseDocument(val documentId: String) : ScratchUiAction
    data class RenameDocument(val documentId: String, val newTitle: String) : ScratchUiAction
    data class UpdateContent(val content: String) : ScratchUiAction
    data class SetDocumentLanguage(val documentId: String, val language: EditorLanguage) : ScratchUiAction

    // Export actions
    data class RequestExportDocument(val documentId: String) : ScratchUiAction
    data class SetUserMessage(val message: String) : ScratchUiAction

    // Lockbox actions
    data class RequestToggleLock(val documentId: String) : ScratchUiAction
    data class ConfirmLockDocument(val documentId: String) : ScratchUiAction
    data object DismissLockConfirmation : ScratchUiAction
    data class AuthenticateDocumentSuccess(val documentId: String) : ScratchUiAction
    data object LockAllDocuments : ScratchUiAction

    // Pane & workspace actions
    data class SetPane(val pane: ScratchPane) : ScratchUiAction
    data object ClearMessage : ScratchUiAction

    // Header Timer actions (preserved)
    data object ToggleTimer : ScratchUiAction
    data object ResetTimer : ScratchUiAction

    // Checklist actions (preserved)
    data class AddChecklistItem(val text: String) : ScratchUiAction
    data class ToggleChecklistItem(val itemId: String) : ScratchUiAction
    data class DeleteChecklistItem(val itemId: String) : ScratchUiAction
    data class UpdateChecklistItemText(val itemId: String, val text: String) : ScratchUiAction
    data object ClearChecklist : ScratchUiAction
}

sealed interface ScratchUiEffect : UiEffect {
    data object TimerFinished : ScratchUiEffect
    data class RequestBiometricAuth(val documentId: String) : ScratchUiEffect
    data class LaunchExportPicker(
        val documentId: String,
        val fileName: String,
        val mimeType: String,
        val content: String
    ) : ScratchUiEffect
}

enum class ScratchPane {
    TEXT,
    CHECKLIST,
}

@HiltViewModel
class ScratchViewModel @Inject constructor(
    private val scratchRepository: IScratchRepository,
) : BaseViewModel<ScratchUiState, ScratchUiAction, ScratchUiEffect>(
    ScratchViewModel.createInitialState(),
) {
    private var timerJob: Job? = null
    private var autoSaveJob: Job? = null
    private var pendingDocId: String? = null
    private var pendingContent: String? = null
    private var pendingExportDocumentId: String? = null

    companion object {
        fun createInitialState(): ScratchUiState =
            ScratchUiState(
                checklistItems = listOf(
                    ChecklistItem("1", "Review PRD gap analysis", isCompleted = false),
                    ChecklistItem("2", "Test StatsPill overlay", isCompleted = false),
                    ChecklistItem("3", "Verify clipboard transformations", isCompleted = true),
                ),
                activePane = ScratchPane.TEXT,
            )
    }

    init {
        viewModelScope.launch {
            scratchRepository.getScratchpads().collect { docs ->
                if (docs.isEmpty()) {
                    val defaultDoc = Scratch(
                        id = "default_scratch_note",
                        title = "Quick Notes",
                        content = "# Welcome to Alloy Lightweight Editor\n\n- Multi-document tabbed workspace\n- Syntax highlighting with 17+ languages\n- Secure Lockbox with biometric protection",
                        language = EditorLanguage.MARKDOWN.name,
                        isLocked = false,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    scratchRepository.insertScratchpad(defaultDoc)
                } else {
                    updateState { state ->
                        val targetActiveId = when {
                            state.activeDocumentId != null && docs.any { it.id == state.activeDocumentId } -> state.activeDocumentId
                            else -> docs.firstOrNull()?.id
                        }
                        state.copy(
                            documents = docs,
                            activeDocumentId = targetActiveId
                        )
                    }
                }
            }
        }
    }

    override fun onAction(action: ScratchUiAction) {
        when (action) {
            is ScratchUiAction.CreateNewDocument -> createNewDocument()
            is ScratchUiAction.SelectDocument -> selectDocument(action.documentId)
            is ScratchUiAction.CloseDocument -> closeDocument(action.documentId)
            is ScratchUiAction.RenameDocument -> renameDocument(action.documentId, action.newTitle)
            is ScratchUiAction.UpdateContent -> updateContent(action.content)
            is ScratchUiAction.SetDocumentLanguage -> setDocumentLanguage(action.documentId, action.language)

            is ScratchUiAction.RequestExportDocument -> requestExportDocument(action.documentId)
            is ScratchUiAction.SetUserMessage -> updateState { it.copy(userMessage = action.message) }

            is ScratchUiAction.RequestToggleLock -> requestToggleLock(action.documentId)
            is ScratchUiAction.ConfirmLockDocument -> confirmLockDocument(action.documentId)
            ScratchUiAction.DismissLockConfirmation -> updateState {
                it.copy(showLockConfirmationDialog = false, pendingLockDocumentId = null)
            }
            is ScratchUiAction.AuthenticateDocumentSuccess -> {
                updateState { it.copy(unlockedDocumentIds = it.unlockedDocumentIds + action.documentId) }
                if (pendingExportDocumentId == action.documentId) {
                    val doc = uiState.value.documents.find { it.id == action.documentId }
                    pendingExportDocumentId = null
                    if (doc != null) {
                        proceedWithExport(doc)
                    }
                }
            }
            ScratchUiAction.LockAllDocuments -> {
                pendingExportDocumentId = null
                updateState { it.copy(unlockedDocumentIds = emptySet()) }
            }

            is ScratchUiAction.SetPane -> updateState { it.copy(activePane = action.pane) }
            ScratchUiAction.ClearMessage -> updateState { it.copy(userMessage = null) }

            ScratchUiAction.ToggleTimer -> {
                if (uiState.value.isTimerRunning) stopTimer() else startTimer()
            }
            ScratchUiAction.ResetTimer -> {
                stopTimer()
                updateState { it.copy(timerSeconds = 0) }
            }

            is ScratchUiAction.AddChecklistItem -> addChecklistItem(action.text)
            is ScratchUiAction.ToggleChecklistItem -> toggleChecklistItem(action.itemId)
            is ScratchUiAction.DeleteChecklistItem -> deleteChecklistItem(action.itemId)
            is ScratchUiAction.UpdateChecklistItemText -> updateChecklistItemText(action.itemId, action.text)
            ScratchUiAction.ClearChecklist -> updateState { it.copy(checklistItems = emptyList()) }
        }
    }

    private fun createNewDocument() {
        savePendingContent()
        val docCount = uiState.value.documents.size + 1
        val newDoc = Scratch(
            id = UUID.randomUUID().toString(),
            title = "Untitled $docCount",
            content = "",
            language = EditorLanguage.PLAIN_TEXT.name,
            isLocked = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            scratchRepository.insertScratchpad(newDoc)
            updateState {
                it.copy(
                    documents = it.documents + newDoc,
                    activeDocumentId = newDoc.id
                )
            }
        }
    }

    private fun selectDocument(docId: String) {
        if (uiState.value.activeDocumentId == docId) return
        savePendingContent()
        updateState { it.copy(activeDocumentId = docId) }
    }

    private fun closeDocument(docId: String) {
        savePendingContent()
        val currentDocs = uiState.value.documents
        val docToClose = currentDocs.find { it.id == docId } ?: return

        viewModelScope.launch {
            scratchRepository.deleteScratchpad(docId)
            val remaining = currentDocs.filter { it.id != docId }
            val nextActiveId = if (uiState.value.activeDocumentId == docId) {
                remaining.firstOrNull()?.id
            } else {
                uiState.value.activeDocumentId
            }
            updateState {
                it.copy(
                    documents = remaining,
                    activeDocumentId = nextActiveId,
                    unlockedDocumentIds = it.unlockedDocumentIds - docId
                )
            }
        }
    }

    private fun renameDocument(docId: String, newTitle: String) {
        val doc = uiState.value.documents.find { it.id == docId } ?: return
        val updated = doc.copy(title = newTitle.trim(), updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            scratchRepository.updateScratchpad(updated)
            updateState { state ->
                state.copy(documents = state.documents.map { if (it.id == docId) updated else it })
            }
        }
    }

    private fun updateContent(content: String) {
        val currentDocId = uiState.value.activeDocumentId ?: return
        updateState { state ->
            val updatedList = state.documents.map { doc ->
                if (doc.id == currentDocId) doc.copy(content = content) else doc
            }
            state.copy(documents = updatedList)
        }
        scheduleAutoSave(currentDocId, content)
    }

    private fun setDocumentLanguage(docId: String, language: EditorLanguage) {
        val doc = uiState.value.documents.find { it.id == docId } ?: return
        val updated = doc.copy(language = language.name, updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            scratchRepository.updateScratchpad(updated)
            updateState { state ->
                state.copy(documents = state.documents.map { if (it.id == docId) updated else it })
            }
        }
    }

    private fun requestExportDocument(documentId: String) {
        savePendingContent()
        val doc = uiState.value.documents.find { it.id == documentId } ?: return
        val isLocked = doc.isLocked && !uiState.value.unlockedDocumentIds.contains(documentId)
        if (isLocked) {
            pendingExportDocumentId = documentId
            sendEffect(ScratchUiEffect.RequestBiometricAuth(documentId))
            return
        }

        proceedWithExport(doc)
    }

    private fun proceedWithExport(doc: Scratch) {
        val language = EditorLanguage.fromName(doc.language)
        val ext = language.extension
        val cleanTitle = doc.title.trim().ifBlank { "Untitled" }
        val fileName = if (cleanTitle.endsWith(".$ext", ignoreCase = true)) cleanTitle else "$cleanTitle.$ext"
        val mimeType = language.mimeType
        val contentToExport = if (doc.id == pendingDocId) pendingContent ?: doc.content else doc.content

        sendEffect(
            ScratchUiEffect.LaunchExportPicker(
                documentId = doc.id,
                fileName = fileName,
                mimeType = mimeType,
                content = contentToExport
            )
        )
    }

    private fun requestToggleLock(docId: String) {
        val doc = uiState.value.documents.find { it.id == docId } ?: return
        if (doc.isLocked) {
            // Unlocking / removing lock
            val updated = doc.copy(isLocked = false, updatedAt = System.currentTimeMillis())
            viewModelScope.launch {
                scratchRepository.updateScratchpad(updated)
                updateState { state ->
                    state.copy(
                        documents = state.documents.map { if (it.id == docId) updated else it },
                        unlockedDocumentIds = state.unlockedDocumentIds - docId,
                        userMessage = "Document removed from Secure Lockbox"
                    )
                }
            }
        } else {
            // Require user confirmation to clearly communicate lockbox parameters
            updateState {
                it.copy(
                    showLockConfirmationDialog = true,
                    pendingLockDocumentId = docId
                )
            }
        }
    }

    private fun confirmLockDocument(docId: String) {
        val doc = uiState.value.documents.find { it.id == docId } ?: return
        val updated = doc.copy(isLocked = true, updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            scratchRepository.updateScratchpad(updated)
            updateState { state ->
                state.copy(
                    documents = state.documents.map { if (it.id == docId) updated else it },
                    unlockedDocumentIds = state.unlockedDocumentIds - docId,
                    showLockConfirmationDialog = false,
                    pendingLockDocumentId = null,
                    userMessage = "Document secured in Lockbox. Biometric/Screen lock required to view."
                )
            }
        }
    }

    private fun scheduleAutoSave(docId: String, content: String) {
        autoSaveJob?.cancel()
        pendingDocId = docId
        pendingContent = content

        autoSaveJob = viewModelScope.launch {
            delay(uiState.value.autoSaveDebounceMs)
            savePendingContent()
        }
    }

    private fun savePendingContent() {
        val docId = pendingDocId ?: return
        val content = pendingContent ?: return
        val doc = uiState.value.documents.find { it.id == docId } ?: return

        val updated = doc.copy(content = content, updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            scratchRepository.updateScratchpad(updated)
        }
        pendingDocId = null
        pendingContent = null
    }

    private fun startTimer() {
        updateState { it.copy(isTimerRunning = true) }
        timerJob = viewModelScope.launch {
            while (uiState.value.isTimerRunning) {
                delay(1000L)
                updateState { it.copy(timerSeconds = it.timerSeconds + 1) }
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        updateState { it.copy(isTimerRunning = false) }
    }

    private fun addChecklistItem(text: String) {
        val newItem = ChecklistItem(
            id = UUID.randomUUID().toString(),
            text = text,
            isCompleted = false,
        )
        updateState { it.copy(checklistItems = it.checklistItems + newItem) }
    }

    private fun toggleChecklistItem(itemId: String) {
        updateState { state ->
            val updated = state.checklistItems.map { item ->
                if (item.id == itemId) item.copy(isCompleted = !item.isCompleted) else item
            }
            state.copy(checklistItems = updated)
        }
    }

    private fun deleteChecklistItem(itemId: String) {
        updateState { it.copy(checklistItems = it.checklistItems.filter { it.id != itemId }) }
    }

    private fun updateChecklistItemText(itemId: String, text: String) {
        updateState { state ->
            val updated = state.checklistItems.map { item ->
                if (item.id == itemId) item.copy(text = text) else item
            }
            state.copy(checklistItems = updated)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        autoSaveJob?.cancel()
        savePendingContent()
    }
}

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

data class ScratchUiState(
    val documents: List<Scratch> = emptyList(),
    val activeDocumentId: String? = null,
    val unlockedDocumentIds: Set<String> = emptySet(),
    val showLockConfirmationDialog: Boolean = false,
    val pendingLockDocumentId: String? = null,
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

    // Workspace actions
    data object ClearMessage : ScratchUiAction
}

sealed interface ScratchUiEffect : UiEffect {
    data class RequestBiometricAuth(val documentId: String) : ScratchUiEffect
    data class LaunchExportPicker(
        val documentId: String,
        val fileName: String,
        val mimeType: String,
        val content: String
    ) : ScratchUiEffect
}

@HiltViewModel
class ScratchViewModel @Inject constructor(
    private val scratchRepository: IScratchRepository,
) : BaseViewModel<ScratchUiState, ScratchUiAction, ScratchUiEffect>(
    ScratchUiState(),
) {
    private var timerJob: Job? = null
    private var autoSaveJob: Job? = null
    private var pendingDocId: String? = null
    private var pendingContent: String? = null
    private var pendingExportDocumentId: String? = null
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

            ScratchUiAction.ClearMessage -> updateState { it.copy(userMessage = null) }
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

    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()
        savePendingContent()
    }
}

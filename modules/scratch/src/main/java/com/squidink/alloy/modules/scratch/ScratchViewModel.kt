package com.squidink.alloy.modules.scratch

import androidx.lifecycle.viewModelScope
import com.squidink.alloy.core.common.BaseViewModel
import com.squidink.alloy.core.common.UiAction
import com.squidink.alloy.core.common.UiEffect
import com.squidink.alloy.core.common.UiState
import com.squidink.alloy.core.domain.repository.IScratchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChecklistItem(
    val id: String,
    val text: String,
    val isCompleted: Boolean = false,
)

data class ScratchUiState(
    val noteContent: String = "",
    val checklistItems: List<ChecklistItem> = emptyList(),
    val activePane: ScratchPane = ScratchPane.TEXT,
    val isTimerRunning: Boolean = false,
    val timerSeconds: Int = 0,
    val isStopwatchRunning: Boolean = false,
    val stopwatchSeconds: Int = 0,
    val stopwatchLaps: List<Int> = emptyList(),
    val autoSaveDebounceMs: Long = 500,
) : UiState

sealed interface ScratchUiAction : UiAction {
    data class UpdateContent(
        val content: String,
    ) : ScratchUiAction

    data object ToggleTimer : ScratchUiAction

    data object ResetTimer : ScratchUiAction

    data object StartStopwatch : ScratchUiAction

    data object StopStopwatch : ScratchUiAction

    data object ResetStopwatch : ScratchUiAction

    data object AddLap : ScratchUiAction

    data class SetPane(
        val pane: ScratchPane,
    ) : ScratchUiAction

    data class AddChecklistItem(
        val text: String,
    ) : ScratchUiAction

    data class ToggleChecklistItem(
        val itemId: String,
    ) : ScratchUiAction

    data class DeleteChecklistItem(
        val itemId: String,
    ) : ScratchUiAction

    data class UpdateChecklistItemText(
        val itemId: String,
        val text: String,
    ) : ScratchUiAction

    data object ClearChecklist : ScratchUiAction
}

sealed interface ScratchUiEffect : UiEffect {
    data object TimerFinished : ScratchUiEffect

    data object StopwatchLapRecorded : ScratchUiEffect
}

enum class ScratchPane {
    TEXT,
    CHECKLIST,
    STOPWATCH,
}

@HiltViewModel
class ScratchViewModel
    @Inject
    constructor(
        private val scratchRepository: IScratchRepository,
    ) : BaseViewModel<ScratchUiState, ScratchUiAction, ScratchUiEffect>(
            ScratchViewModel.createInitialState(),
        ) {
        private var timerJob: Job? = null
        private var stopwatchJob: Job? = null
        private var autoSaveJob: Job? = null
        private var pendingContent: String = ""

        companion object {
            fun createInitialState(): ScratchUiState =
                ScratchUiState(
                    noteContent = "# Quick Notes\n- Deploy Alloy to desktop emulator\n- Validate MVI viewmodel contracts",
                    checklistItems =
                        listOf(
                            ChecklistItem("1", "Review PRD gap analysis", isCompleted = false),
                            ChecklistItem("2", "Test StatsPill overlay", isCompleted = false),
                            ChecklistItem("3", "Verify clipboard transformations", isCompleted = true),
                        ),
                    activePane = ScratchPane.TEXT,
                )
        }

        init {
            viewModelScope.launch {
                scratchRepository.getScratchpads().collect { scratchpads ->
                    val firstNote = scratchpads.firstOrNull()
                    if (firstNote != null) {
                        updateState { it.copy(noteContent = firstNote.content) }
                    }
                }
            }
        }

        override fun onAction(action: ScratchUiAction) {
            when (action) {
                is ScratchUiAction.UpdateContent -> {
                    updateState { it.copy(noteContent = action.content) }
                    scheduleAutoSave(action.content)
                }

                ScratchUiAction.ToggleTimer -> {
                    val currentState = uiState.value
                    if (currentState.isTimerRunning) stopTimer() else startTimer()
                }

                ScratchUiAction.ResetTimer -> {
                    stopTimer()
                    updateState { it.copy(timerSeconds = 0) }
                }

                ScratchUiAction.StartStopwatch -> {
                    val currentState = uiState.value
                    if (!currentState.isStopwatchRunning) startStopwatch()
                }

                ScratchUiAction.StopStopwatch -> {
                    stopStopwatch()
                }

                ScratchUiAction.ResetStopwatch -> {
                    stopStopwatch()
                    updateState { it.copy(stopwatchSeconds = 0, stopwatchLaps = emptyList()) }
                }

                ScratchUiAction.AddLap -> {
                    addLap()
                }

                is ScratchUiAction.SetPane -> {
                    updateState { it.copy(activePane = action.pane) }
                }

                is ScratchUiAction.AddChecklistItem -> {
                    addChecklistItem(action.text)
                }

                is ScratchUiAction.ToggleChecklistItem -> {
                    toggleChecklistItem(action.itemId)
                }

                is ScratchUiAction.DeleteChecklistItem -> {
                    deleteChecklistItem(action.itemId)
                }

                is ScratchUiAction.UpdateChecklistItemText -> {
                    updateChecklistItemText(action.itemId, action.text)
                }

                ScratchUiAction.ClearChecklist -> {
                    updateState { it.copy(checklistItems = emptyList()) }
                }
            }
        }

        private fun scheduleAutoSave(content: String) {
            val currentState = uiState.value
            autoSaveJob?.cancel()
            pendingContent = content

            autoSaveJob =
                viewModelScope.launch {
                    delay(currentState.autoSaveDebounceMs)
                    savePendingContent()
                }
        }

        private fun savePendingContent() {
            if (pendingContent.isNotEmpty()) {
                viewModelScope.launch {
                    val scratchpad = com.squidink.alloy.core.domain.repository.Scratch(
                        id = "default_scratch_note",
                        content = pendingContent,
                        label = "Default Note",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    scratchRepository.insertScratchpad(scratchpad)
                }
                pendingContent = ""
            }
        }

        private fun startTimer() {
            updateState { it.copy(isTimerRunning = true) }
            timerJob =
                viewModelScope.launch {
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

        private fun startStopwatch() {
            updateState { it.copy(isStopwatchRunning = true) }
            stopwatchJob =
                viewModelScope.launch {
                    while (uiState.value.isStopwatchRunning) {
                        delay(1000L)
                        updateState { it.copy(stopwatchSeconds = it.stopwatchSeconds + 1) }
                    }
                }
        }

        private fun stopStopwatch() {
            stopwatchJob?.cancel()
            stopwatchJob = null
            updateState { it.copy(isStopwatchRunning = false) }
        }

        private fun addLap() {
            val currentState = uiState.value
            val currentSeconds = currentState.stopwatchSeconds
            updateState { it.copy(stopwatchLaps = it.stopwatchLaps + currentSeconds) }
            sendEffect(ScratchUiEffect.StopwatchLapRecorded)
        }

        private fun addChecklistItem(text: String) {
            val newItem =
                ChecklistItem(
                    id =
                        java.util.UUID
                            .randomUUID()
                            .toString(),
                    text = text,
                    isCompleted = false,
                )
            updateState { it.copy(checklistItems = it.checklistItems + newItem) }
        }

        private fun toggleChecklistItem(itemId: String) {
            updateState { state ->
                val updated =
                    state.checklistItems.map { item ->
                        if (item.id == itemId) item.copy(isCompleted = !item.isCompleted) else item
                    }
                state.copy(checklistItems = updated)
            }
        }

        private fun deleteChecklistItem(itemId: String) {
            updateState { it.copy(checklistItems = it.checklistItems.filter { it.id != itemId }) }
        }

        private fun updateChecklistItemText(
            itemId: String,
            text: String,
        ) {
            updateState { state ->
                val updated =
                    state.checklistItems.map { item ->
                        if (item.id == itemId) item.copy(text = text) else item
                    }
                state.copy(checklistItems = updated)
            }
        }

        override fun onCleared() {
            super.onCleared()
            stopTimer()
            stopStopwatch()
            autoSaveJob?.cancel()
            savePendingContent()
        }
    }

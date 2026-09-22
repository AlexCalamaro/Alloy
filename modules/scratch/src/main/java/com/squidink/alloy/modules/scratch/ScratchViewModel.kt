package com.squidink.alloy.modules.scratch

import androidx.lifecycle.viewModelScope
import com.squidink.alloy.core.common.BaseViewModel
import com.squidink.alloy.core.common.UiAction
import com.squidink.alloy.core.common.UiEffect
import com.squidink.alloy.core.common.UiState
import com.squidink.alloy.modules.scratch.db.ScratchDao
import com.squidink.alloy.modules.scratch.db.ScratchEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScratchUiState(
    val noteContent: String = "",
    val isTimerRunning: Boolean = false,
    val timerSeconds: Int = 0
) : UiState

sealed interface ScratchUiAction : UiAction {
    data class UpdateContent(val content: String) : ScratchUiAction
    data object ToggleTimer : ScratchUiAction
    data object ResetTimer : ScratchUiAction
}

sealed interface ScratchUiEffect : UiEffect {
    data object TimerFinished : ScratchUiEffect
}

@HiltViewModel
class ScratchViewModel @Inject constructor(
    private val scratchDao: ScratchDao
) : BaseViewModel<ScratchUiState, ScratchUiAction, ScratchUiEffect>(
    ScratchUiState(noteContent = "# Quick Notes\n- Deploy Alloy to desktop emulator\n- Validate MVI viewmodel contracts")
) {

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            scratchDao.getAllNotes().collect { notes ->
                val firstNote = notes.firstOrNull()
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
                viewModelScope.launch {
                    scratchDao.insertNote(
                        ScratchEntity(
                            id = "default_scratch_note",
                            content = action.content,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            ScratchUiAction.ToggleTimer -> {
                if (uiState.value.isTimerRunning) stopTimer() else startTimer()
            }
            ScratchUiAction.ResetTimer -> {
                stopTimer()
                updateState { it.copy(timerSeconds = 0) }
            }
        }
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

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}

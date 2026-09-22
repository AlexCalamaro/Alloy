package com.squidink.alloy.core.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Interface representing immutable UI State in the MVI architecture.
 */
interface UiState

/**
 * Interface representing user actions/intents in the MVI architecture.
 */
interface UiAction

/**
 * Interface representing single-shot side-effects (e.g. navigation, toasts).
 */
interface UiEffect

/**
 * Abstract BaseViewModel establishing the MVI contract across all Alloy feature modules.
 */
abstract class BaseViewModel<State : UiState, Action : UiAction, Effect : UiEffect>(
    initialState: State
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    /**
     * Entry point for dispatching user actions to the ViewModel.
     */
    abstract fun onAction(action: Action)

    /**
     * Updates the current state using a reducer function.
     */
    protected fun updateState(reducer: (State) -> State) {
        _uiState.value = reducer(_uiState.value)
    }

    /**
     * Emits a single-shot UI Effect.
     */
    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}

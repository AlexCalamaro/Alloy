package com.squidink.alloy.modules.settings

import androidx.lifecycle.viewModelScope
import com.squidink.alloy.core.common.BaseViewModel
import com.squidink.alloy.core.common.UiAction
import com.squidink.alloy.core.common.UiEffect
import com.squidink.alloy.core.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = false,
) : UiState

sealed interface SettingsUiAction : UiAction {
    data object Refresh : SettingsUiAction
}

sealed interface SettingsUiEffect : UiEffect

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
    ) : BaseViewModel<SettingsUiState, SettingsUiAction, SettingsUiEffect>(
            SettingsUiState(),
        ) {

    init {
        // Initialize view model
    }

    override fun onAction(action: SettingsUiAction) {
        when (action) {
            is SettingsUiAction.Refresh -> refreshData()
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            // Implement refresh logic here
        }
    }
}

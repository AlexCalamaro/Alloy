package com.squidink.alloy.modules.settings

import androidx.lifecycle.viewModelScope
import com.squidink.alloy.core.common.BaseViewModel
import com.squidink.alloy.core.common.UiAction
import com.squidink.alloy.core.common.UiEffect
import com.squidink.alloy.core.common.UiState
import com.squidink.alloy.core.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = false,
    val dynamicColor: Boolean = true,
    val usePercentages: Boolean = true,
    val cacheEnabled: Boolean = true,
) : UiState

sealed interface SettingsUiAction : UiAction {
    data object Refresh : SettingsUiAction
    data class SetDynamicColor(val enabled: Boolean) : SettingsUiAction
    data class SetUsePercentages(val enabled: Boolean) : SettingsUiAction
    data class SetCacheEnabled(val enabled: Boolean) : SettingsUiAction
}

sealed interface SettingsUiEffect : UiEffect {
    data class ShowToast(val message: String) : SettingsUiEffect
    data object SettingsSaved : SettingsUiEffect
    data object SettingsLoadError : SettingsUiEffect
}

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
    ) : BaseViewModel<SettingsUiState, SettingsUiAction, SettingsUiEffect>(
            SettingsUiState(),
        ) {

    init {
        observeSettings()
    }

    private fun observeSettings() {
        settingsRepository.observeDynamicColor().onEach { dynamicColor ->
            updateState { it.copy(dynamicColor = dynamicColor) }
        }.launchIn(viewModelScope)

        settingsRepository.observeUsePercentages().onEach { usePercentages ->
            updateState { it.copy(usePercentages = usePercentages) }
        }.launchIn(viewModelScope)

        settingsRepository.observeCacheEnabled().onEach { cacheEnabled ->
            updateState { it.copy(cacheEnabled = cacheEnabled) }
        }.launchIn(viewModelScope)
    }

    override fun onAction(action: SettingsUiAction) {
        when (action) {
            is SettingsUiAction.Refresh -> observeSettings()
            is SettingsUiAction.SetDynamicColor -> {
                viewModelScope.launch {
                    settingsRepository.setDynamicColor(action.enabled)
                    updateState { it.copy(dynamicColor = action.enabled) }
                    sendEffect(SettingsUiEffect.SettingsSaved)
                }
            }
            is SettingsUiAction.SetUsePercentages -> {
                viewModelScope.launch {
                    settingsRepository.setUsePercentages(action.enabled)
                    updateState { it.copy(usePercentages = action.enabled) }
                    sendEffect(SettingsUiEffect.SettingsSaved)
                }
            }
            is SettingsUiAction.SetCacheEnabled -> {
                viewModelScope.launch {
                    settingsRepository.setCacheEnabled(action.enabled)
                    updateState { it.copy(cacheEnabled = action.enabled) }
                    sendEffect(SettingsUiEffect.SettingsSaved)
                }
            }
        }
    }
}

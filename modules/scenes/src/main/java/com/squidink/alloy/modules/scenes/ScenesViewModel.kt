package com.squidink.alloy.modules.scenes

import android.content.Context
import com.squidink.alloy.core.common.BaseViewModel
import com.squidink.alloy.core.common.UiAction
import com.squidink.alloy.core.common.UiEffect
import com.squidink.alloy.core.common.UiState
import com.squidink.alloy.modules.scenes.model.PlacementHint
import com.squidink.alloy.modules.scenes.model.Scene
import com.squidink.alloy.modules.scenes.model.SceneStep
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class ScenesUiState(
    val scenes: List<Scene> = emptyList(),
    val selectedScene: Scene? = null
) : UiState

sealed interface ScenesUiAction : UiAction {
    data class FireScene(val context: Context, val sceneId: String) : ScenesUiAction
    data class AddScene(val scene: Scene) : ScenesUiAction
    data class DeleteScene(val sceneId: String) : ScenesUiAction
}

sealed interface ScenesUiEffect : UiEffect {
    data class ShowToast(val message: String) : ScenesUiEffect
}

@HiltViewModel
class ScenesViewModel @Inject constructor(
    private val sceneLauncher: SceneLauncher
) : BaseViewModel<ScenesUiState, ScenesUiAction, ScenesUiEffect>(
    ScenesUiState(scenes = defaultScenes())
) {

    override fun onAction(action: ScenesUiAction) {
        when (action) {
            is ScenesUiAction.FireScene -> {
                val scene = uiState.value.scenes.find { it.id == action.sceneId }
                if (scene != null) {
                    val success = sceneLauncher.launchScene(action.context, scene)
                    val msg = if (success) "Fired scene: ${scene.name}" else "Failed to fire scene: ${scene.name}"
                    sendEffect(ScenesUiEffect.ShowToast(msg))
                }
            }
            is ScenesUiAction.AddScene -> {
                updateState { it.copy(scenes = it.scenes + action.scene) }
            }
            is ScenesUiAction.DeleteScene -> {
                updateState { it.copy(scenes = it.scenes.filterNot { s -> s.id == action.sceneId }) }
            }
        }
    }

    companion object {
        fun defaultScenes(): List<Scene> = listOf(
            Scene(
                id = "coding_stack",
                name = "Coding Stack",
                description = "Launch IDE and terminal side-by-side",
                steps = listOf(
                    SceneStep("1", "com.squidink.alloy", placement = PlacementHint.LEFT)
                )
            )
        )
    }
}

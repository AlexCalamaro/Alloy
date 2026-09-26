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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class ScenesUiState(
    val scenes: List<Scene> = emptyList(),
    val selectedScene: Scene? = null
) : UiState

sealed interface ScenesUiAction : UiAction {
    data class FireScene(val sceneId: String) : ScenesUiAction
    data class AddScene(val scene: Scene) : ScenesUiAction
    data class DeleteScene(val sceneId: String) : ScenesUiAction
}

sealed interface ScenesUiEffect : UiEffect {
    data class ShowToast(val message: String) : ScenesUiEffect
}

@HiltViewModel
class ScenesViewModel @Inject constructor(
    private val sceneLauncher: SceneLauncher,
    @ApplicationContext private val context: Context
) : BaseViewModel<ScenesUiState, ScenesUiAction, ScenesUiEffect>(
    ScenesUiState(scenes = defaultScenes())
) {

    override fun onAction(action: ScenesUiAction) {
        when (action) {
            is ScenesUiAction.FireScene -> {
                val currentState = uiState.value
                val scene = currentState.scenes.find { it.id == action.sceneId }
                if (scene != null) {
                    val success = sceneLauncher.launchScene(context, scene)
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
                description = "IDE and terminal side-by-side for development",
                steps = listOf(
                    SceneStep("1", "com.squidink.alloy", placement = PlacementHint.LEFT),
                    SceneStep("2", "com.termux", placement = PlacementHint.RIGHT)
                )
            ),
            Scene(
                id = "research_workspace",
                name = "Research Workspace",
                description = "Browser + notes for research and documentation",
                steps = listOf(
                    SceneStep("1", "com.android.chrome", activityName = "com.android.chrome.browser.main.MainActivity", placement = PlacementHint.LEFT),
                    SceneStep("2", "com.squidink.alloy.modules.scratch.ScratchActivity", placement = PlacementHint.RIGHT)
                )
            ),
            Scene(
                id = "video_call_setup",
                name = "Video Call Setup",
                description = "Video app + chat + notes for meetings",
                steps = listOf(
                    SceneStep("1", "us.zoom.videomeetings", placement = PlacementHint.CENTER),
                    SceneStep("2", "com.google.android.gm", placement = PlacementHint.RIGHT)
                )
            ),
            Scene(
                id = "media_consumption",
                name = "Media Consumption",
                description = "Video player + music for entertainment",
                steps = listOf(
                    SceneStep("1", "com.google.android.youtube", placement = PlacementHint.LEFT),
                    SceneStep("2", "com.spotify.music", placement = PlacementHint.RIGHT)
                )
            ),
            Scene(
                id = "file_management",
                name = "File Management",
                description = "File manager + text editor for file work",
                steps = listOf(
                    SceneStep("1", "com.android.documentsui", placement = PlacementHint.LEFT),
                    SceneStep("2", "com.squidink.alloy.modules.scratch.ScratchActivity", placement = PlacementHint.RIGHT)
                )
            )
        )
    }
}

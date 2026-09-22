package com.squidink.alloy.modules.clip

import androidx.lifecycle.viewModelScope
import com.squidink.alloy.core.common.BaseViewModel
import com.squidink.alloy.core.common.UiAction
import com.squidink.alloy.core.common.UiEffect
import com.squidink.alloy.core.common.UiState
import com.squidink.alloy.modules.clip.db.ClipDao
import com.squidink.alloy.modules.clip.db.ClipEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClipUiState(
    val searchQuery: String = "",
    val clips: List<ClipEntity> = emptyList(),
    val selectedClip: ClipEntity? = null
) : UiState

sealed interface ClipUiAction : UiAction {
    data class UpdateSearchQuery(val query: String) : ClipUiAction
    data class SelectClip(val clip: ClipEntity?) : ClipUiAction
    data class ApplyTransformation(val type: TransformationType) : ClipUiAction
    data class AddClip(val text: String, val app: String = "Chrome") : ClipUiAction
}

enum class TransformationType {
    UPPERCASE,
    LOWERCASE,
    TRIM
}

sealed interface ClipUiEffect : UiEffect {
    data class CopyToClipboard(val text: String) : ClipUiEffect
}

@HiltViewModel
class ClipViewModel @Inject constructor(
    private val clipDao: ClipDao? = null
) : BaseViewModel<ClipUiState, ClipUiAction, ClipUiEffect>(
    ClipUiState(
        clips = listOf(
            ClipEntity("1", "https://github.com/squidink/alloy", sourceApp = "Chrome"),
            ClipEntity("2", "val apiKey = \"secret_12345\"", sourceApp = "DeskTerm")
        )
    )
) {

    init {
        clipDao?.let { dao ->
            viewModelScope.launch {
                dao.getAllClips().collect { items ->
                    if (items.isNotEmpty()) {
                        updateState { currentState -> currentState.copy(clips = items) }
                    }
                }
            }
        }
    }

    override fun onAction(action: ClipUiAction) {
        when (action) {
            is ClipUiAction.UpdateSearchQuery -> {
                updateState { it.copy(searchQuery = action.query) }
            }
            is ClipUiAction.SelectClip -> {
                updateState { it.copy(selectedClip = action.clip) }
            }
            is ClipUiAction.ApplyTransformation -> {
                val selected = uiState.value.selectedClip ?: return
                val transformedText = when (action.type) {
                    TransformationType.UPPERCASE -> ClipTransformations.toUpperCase(selected.textContent)
                    TransformationType.LOWERCASE -> ClipTransformations.toLowerCase(selected.textContent)
                    TransformationType.TRIM -> ClipTransformations.trimWhitespace(selected.textContent)
                }
                sendEffect(ClipUiEffect.CopyToClipboard(transformedText))
            }
            is ClipUiAction.AddClip -> {
                val newClip = ClipEntity(
                    id = System.currentTimeMillis().toString(),
                    textContent = action.text,
                    sourceApp = action.app
                )
                updateState { it.copy(clips = listOf(newClip) + it.clips) }
                viewModelScope.launch {
                    clipDao?.insertClip(newClip)
                }
            }
        }
    }
}

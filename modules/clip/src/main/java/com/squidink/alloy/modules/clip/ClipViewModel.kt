package com.squidink.alloy.modules.clip

import androidx.lifecycle.viewModelScope
import com.squidink.alloy.core.common.BaseViewModel
import com.squidink.alloy.core.common.UiAction
import com.squidink.alloy.core.common.UiEffect
import com.squidink.alloy.core.common.UiState
import com.squidink.alloy.core.domain.repository.Clip
import com.squidink.alloy.core.domain.repository.IClipRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClipUiState(
    val searchQuery: String = "",
    val clips: List<Clip> = emptyList(),
    val selectedClip: Clip? = null,
    val showPinnedOnly: Boolean = false,
) : UiState

sealed interface ClipUiAction : UiAction {
    data class UpdateSearchQuery(
        val query: String,
    ) : ClipUiAction

    data class SelectClip(
        val clip: Clip?,
    ) : ClipUiAction

    data class ApplyTransformation(
        val type: TransformationType,
    ) : ClipUiAction

    data class AddClip(
        val text: String,
        val sourceApp: String = "Unknown",
    ) : ClipUiAction

    data class TogglePin(
        val clipId: String,
    ) : ClipUiAction

    data class DeleteClip(
        val clipId: String,
    ) : ClipUiAction

    data class TogglePinnedOnly(
        val showOnlyPinned: Boolean,
    ) : ClipUiAction
}

enum class TransformationType {
    UPPER_CASE,
    LOWER_CASE,
    TRIM,
    SORT_LINES_ASC,
    SORT_LINES_DESC,
    DEDUPE_LINES,
    JSON_PRETTY,
    JSON_MINIFY,
    BASE64_ENCODE,
    BASE64_DECODE,
    URL_ENCODE,
    URL_DECODE,
}

sealed interface ClipUiEffect : UiEffect {
    data class CopyToClipboard(
        val text: String,
    ) : ClipUiEffect

    data class ShowToast(
        val message: String,
    ) : ClipUiEffect
}

@HiltViewModel
class ClipViewModel
    @Inject
    constructor(
        private val clipRepository: IClipRepository,
    ) : BaseViewModel<ClipUiState, ClipUiAction, ClipUiEffect>(
            ClipUiState(
                clips = emptyList(), // Removed hardcoded test data
            ),
        ) {
        init {
            // Observe clips from repository (domain layer)
            viewModelScope.launch {
                clipRepository.getClips().collect { clips ->
                    updateState { currentState ->
                        val filtered =
                            if (currentState.showPinnedOnly) {
                                clips.filter { it.isPinned }
                            } else {
                                clips
                            }
                        currentState.copy(clips = filtered)
                    }
                }
            }

            // Cleanup task disabled for now
            // Can be enabled later with proper configuration
        }

        override fun onAction(action: ClipUiAction) {
            when (action) {
                is ClipUiAction.UpdateSearchQuery -> {
                    updateState { it.copy(searchQuery = action.query) }
                    filterClipsBySearch(action.query)
                }

                is ClipUiAction.SelectClip -> {
                    updateState { it.copy(selectedClip = action.clip) }
                    if (action.clip != null) {
                        sendEffect(ClipUiEffect.CopyToClipboard(action.clip.textContent))
                    }
                }

                is ClipUiAction.ApplyTransformation -> {
                    uiState.value.selectedClip?.let { clip ->
                        val transformed = transformText(clip.textContent, action.type)
                        sendEffect(ClipUiEffect.CopyToClipboard(transformed))
                        sendEffect(ClipUiEffect.ShowToast("Transformation applied: ${action.type}"))
                    }
                }

                is ClipUiAction.AddClip -> {
                    viewModelScope.launch {
                        val newClip =
                            Clip(
                                id = java.util.UUID.randomUUID().toString(),
                                textContent = action.text,
                                sourceApp = action.sourceApp,
                                isPinned = false,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis(),
                            )
                        clipRepository.insertClip(newClip) // Use repository
                        sendEffect(ClipUiEffect.ShowToast("Clip added"))
                    }
                }

                is ClipUiAction.TogglePin -> {
                    viewModelScope.launch {
                        val clip = uiState.value.clips.find { it.id == action.clipId }
                        clip?.let {
                            // Update via repository
                            val updatedClip = it.copy(isPinned = !it.isPinned)
                            clipRepository.updateClip(updatedClip)
                            sendEffect(ClipUiEffect.ShowToast(if (!it.isPinned) "Pinned" else "Unpinned"))
                        }
                    }
                }

                is ClipUiAction.DeleteClip -> {
                    viewModelScope.launch {
                        clipRepository.deleteClip(action.clipId) // Use repository
                        sendEffect(ClipUiEffect.ShowToast("Clip deleted"))
                    }
                }

                is ClipUiAction.TogglePinnedOnly -> {
                    updateState { it.copy(showPinnedOnly = action.showOnlyPinned) }
                    filterClipsBySearch(uiState.value.searchQuery)
                }
            }
        }

        private fun transformText(
            text: String,
            type: TransformationType,
        ): String =
            when (type) {
                TransformationType.UPPER_CASE -> ClipTransformations.toUpperCase(text)
                TransformationType.LOWER_CASE -> ClipTransformations.toLowerCase(text)
                TransformationType.TRIM -> ClipTransformations.trimWhitespace(text)
                TransformationType.SORT_LINES_ASC -> ClipTransformations.sortLines(text, descending = false)
                TransformationType.SORT_LINES_DESC -> ClipTransformations.sortLines(text, descending = true)
                TransformationType.DEDUPE_LINES -> ClipTransformations.dedupeLines(text)
                TransformationType.JSON_PRETTY -> ClipTransformations.jsonPretty(text)
                TransformationType.JSON_MINIFY -> ClipTransformations.jsonMinify(text)
                TransformationType.BASE64_ENCODE -> ClipTransformations.encodeBase64(text)
                TransformationType.BASE64_DECODE -> ClipTransformations.decodeBase64(text)
                TransformationType.URL_ENCODE -> ClipTransformations.urlEncode(text)
                TransformationType.URL_DECODE -> ClipTransformations.urlDecode(text)
            }

        private fun filterClipsBySearch(query: String) {
            val filtered =
                if (query.isBlank()) {
                    uiState.value.clips
                } else {
                    uiState.value.clips.filter {
                        it.textContent.contains(query, ignoreCase = true) ||
                            it.sourceApp.contains(query, ignoreCase = true)
                    }
                }
            updateState { it.copy(clips = filtered) }
        }
    }

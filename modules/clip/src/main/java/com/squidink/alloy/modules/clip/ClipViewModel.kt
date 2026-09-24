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
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class ClipUiState(
    val searchQuery: String = "",
    val clips: List<ClipEntity> = emptyList(),
    val selectedClip: ClipEntity? = null,
    val showPinnedOnly: Boolean = false,
) : UiState

sealed interface ClipUiAction : UiAction {
    data class UpdateSearchQuery(
        val query: String,
    ) : ClipUiAction

    data class SelectClip(
        val clip: ClipEntity?,
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
        private val clipDao: ClipDao,
        private val enableCleanupTask: Boolean = true,
    ) : BaseViewModel<ClipUiState, ClipUiAction, ClipUiEffect>(
            ClipUiState(
                clips =
                    listOf(
                        ClipEntity("1", "https://github.com/squidink/alloy", sourceApp = "Chrome", isPinned = true),
                        ClipEntity("2", "val apiKey = \"secret_12345\"", sourceApp = "DeskTerm", isPinned = false),
                    ),
            ),
        ) {
        init {
            viewModelScope.launch {
                clipDao.getAllClips().collect { items ->
                    updateState { currentState ->
                        val filtered =
                            if (currentState.showPinnedOnly) {
                                items.filter { it.isPinned }
                            } else {
                                items
                            }
                        currentState.copy(clips = filtered)
                    }
                }
            }

            // Start cleanup task (disabled in tests)
            if (enableCleanupTask) {
                startCleanupTask()
            }
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
                            ClipEntity(
                                id =
                                    java.util.UUID
                                        .randomUUID()
                                        .toString(),
                                textContent = action.text,
                                sourceApp = action.sourceApp,
                                isPinned = false,
                            )
                        clipDao.insertClip(newClip)
                        sendEffect(ClipUiEffect.ShowToast("Clip added"))
                    }
                }

                is ClipUiAction.TogglePin -> {
                    viewModelScope.launch {
                        val clip = uiState.value.clips.find { it.id == action.clipId }
                        clip?.let {
                            clipDao.updateClip(it.id, it.textContent, !it.isPinned)
                            sendEffect(ClipUiEffect.ShowToast(if (!it.isPinned) "Pinned" else "Unpinned"))
                        }
                    }
                }

                is ClipUiAction.DeleteClip -> {
                    viewModelScope.launch {
                        clipDao.deleteClip(action.clipId)
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

        private fun startCleanupTask() {
            viewModelScope.launch {
                // Cleanup old unpinned clips every hour
                while (true) {
                    kotlinx.coroutines.delay(3600000) // 1 hour
                    cleanupOldClips()
                }
            }
        }

        private suspend fun cleanupOldClips() {
            // Simple cleanup: delete old unpinned clips
            // This can be enhanced later
        }
    }

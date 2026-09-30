package com.squidink.alloy.modules.lists

import androidx.lifecycle.viewModelScope
import com.squidink.alloy.core.common.BaseViewModel
import com.squidink.alloy.core.common.UiAction
import com.squidink.alloy.core.common.UiEffect
import com.squidink.alloy.core.common.UiState
import com.squidink.alloy.modules.lists.domain.model.KeepList
import com.squidink.alloy.modules.lists.domain.model.KeepListItem
import com.squidink.alloy.modules.lists.domain.model.ListWithItems
import com.squidink.alloy.modules.lists.domain.usecase.DeleteListUseCase
import com.squidink.alloy.modules.lists.domain.usecase.GetListsWithItemsUseCase
import com.squidink.alloy.modules.lists.domain.usecase.SaveListUseCase
import com.squidink.alloy.modules.lists.domain.usecase.ToggleListItemUseCase
import com.squidink.alloy.modules.lists.domain.usecase.ToggleListPinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ListsUiState(
    val lists: List<ListWithItems> = emptyList(),
    val isEditorOpen: Boolean = false,
    val activeEditorList: KeepList? = null,
    val activeEditorItems: List<KeepListItem> = emptyList(),
    val isLoading: Boolean = false,
    val userMessage: String? = null
) : UiState {
    val pinnedLists: List<ListWithItems>
        get() = lists.filter { it.list.isPinned }

    val otherLists: List<ListWithItems>
        get() = lists.filter { !it.list.isPinned }
}

sealed interface ListsUiAction : UiAction {
    data object OpenCreateListDialog : ListsUiAction
    data class OpenEditListDialog(val listId: String) : ListsUiAction
    data class DismissEditor(val pendingItemText: String = "") : ListsUiAction
    data class UpdateEditorTitle(val title: String) : ListsUiAction
    data class AddEditorItem(val text: String) : ListsUiAction
    data class InsertEditorItemAfter(val currentItemId: String, val newId: String = UUID.randomUUID().toString()) : ListsUiAction
    data class UpdateEditorItemText(val itemId: String, val text: String) : ListsUiAction
    data class ToggleEditorItemCompletion(val itemId: String) : ListsUiAction
    data class DeleteEditorItem(val itemId: String) : ListsUiAction
    data class SetEditorColor(val colorHex: Long) : ListsUiAction
    data object ToggleEditorPin : ListsUiAction
    data class SaveActiveList(val pendingItemText: String = "") : ListsUiAction
    data object DeleteActiveList : ListsUiAction
    data class ToggleListItemQuick(val itemId: String, val isCompleted: Boolean) : ListsUiAction
    data class ToggleListPinQuick(val listId: String, val currentPinState: Boolean) : ListsUiAction
    data class DeleteListQuick(val listId: String) : ListsUiAction
    data object ClearUserMessage : ListsUiAction
}

sealed interface ListsUiEffect : UiEffect {
    data class ShowSnackbar(val message: String) : ListsUiEffect
}

@HiltViewModel
class ListsViewModel @Inject constructor(
    private val getListsWithItemsUseCase: GetListsWithItemsUseCase,
    private val saveListUseCase: SaveListUseCase,
    private val deleteListUseCase: DeleteListUseCase,
    private val toggleListItemUseCase: ToggleListItemUseCase,
    private val toggleListPinUseCase: ToggleListPinUseCase,
) : BaseViewModel<ListsUiState, ListsUiAction, ListsUiEffect>(ListsUiState()) {

    init {
        observeLists()
    }

    private fun observeLists() {
        getListsWithItemsUseCase()
            .onEach { listsWithItems ->
                updateState { it.copy(lists = listsWithItems, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    override fun onAction(action: ListsUiAction) {
        when (action) {
            is ListsUiAction.OpenCreateListDialog -> {
                val newList = KeepList(
                    id = UUID.randomUUID().toString(),
                    title = "",
                    colorHex = 0x00000000L,
                    isPinned = false
                )
                updateState {
                    it.copy(
                        isEditorOpen = true,
                        activeEditorList = newList,
                        activeEditorItems = emptyList()
                    )
                }
            }

            is ListsUiAction.OpenEditListDialog -> {
                val target = uiState.value.lists.find { it.list.id == action.listId }
                if (target != null) {
                    updateState {
                        it.copy(
                            isEditorOpen = true,
                            activeEditorList = target.list,
                            activeEditorItems = target.items
                        )
                    }
                }
            }

            is ListsUiAction.DismissEditor -> {
                saveActiveEditor(action.pendingItemText, showSnackbar = false)
            }

            is ListsUiAction.UpdateEditorTitle -> {
                val current = uiState.value.activeEditorList ?: return
                updateState { it.copy(activeEditorList = current.copy(title = action.title)) }
            }

            is ListsUiAction.AddEditorItem -> {
                val currentList = uiState.value.activeEditorList ?: return
                val trimmed = action.text.trim()
                if (trimmed.isEmpty()) return

                val newItem = KeepListItem(
                    id = UUID.randomUUID().toString(),
                    listId = currentList.id,
                    text = trimmed,
                    isCompleted = false,
                    orderIndex = uiState.value.activeEditorItems.size
                )
                updateState {
                    it.copy(activeEditorItems = it.activeEditorItems + newItem)
                }
            }

            is ListsUiAction.InsertEditorItemAfter -> {
                val currentList = uiState.value.activeEditorList ?: return
                val items = uiState.value.activeEditorItems
                val currentIndex = items.indexOfFirst { it.id == action.currentItemId }
                val insertIndex = if (currentIndex != -1) currentIndex + 1 else items.size
                val newItem = KeepListItem(
                    id = action.newId,
                    listId = currentList.id,
                    text = "",
                    isCompleted = false,
                    orderIndex = insertIndex
                )
                val mutable = items.toMutableList()
                mutable.add(insertIndex, newItem)
                val reindexed = mutable.mapIndexed { index, item -> item.copy(orderIndex = index) }
                updateState { it.copy(activeEditorItems = reindexed) }
            }

            is ListsUiAction.UpdateEditorItemText -> {
                updateState { state ->
                    val updatedItems = state.activeEditorItems.map { item ->
                        if (item.id == action.itemId) item.copy(text = action.text) else item
                    }
                    state.copy(activeEditorItems = updatedItems)
                }
            }

            is ListsUiAction.ToggleEditorItemCompletion -> {
                updateState { state ->
                    val updatedItems = state.activeEditorItems.map { item ->
                        if (item.id == action.itemId) item.copy(isCompleted = !item.isCompleted) else item
                    }
                    state.copy(activeEditorItems = updatedItems)
                }
            }

            is ListsUiAction.DeleteEditorItem -> {
                updateState { state ->
                    val updatedItems = state.activeEditorItems.filter { it.id != action.itemId }
                    state.copy(activeEditorItems = updatedItems)
                }
            }

            is ListsUiAction.SetEditorColor -> {
                val current = uiState.value.activeEditorList ?: return
                updateState { it.copy(activeEditorList = current.copy(colorHex = action.colorHex)) }
            }

            is ListsUiAction.ToggleEditorPin -> {
                val current = uiState.value.activeEditorList ?: return
                updateState { it.copy(activeEditorList = current.copy(isPinned = !current.isPinned)) }
            }

            is ListsUiAction.SaveActiveList -> {
                saveActiveEditor(action.pendingItemText, showSnackbar = true)
            }

            is ListsUiAction.DeleteActiveList -> {
                val currentList = uiState.value.activeEditorList ?: return
                viewModelScope.launch {
                    deleteListUseCase(currentList.id)
                    sendEffect(ListsUiEffect.ShowSnackbar("List deleted"))
                }
                updateState {
                    it.copy(
                        isEditorOpen = false,
                        activeEditorList = null,
                        activeEditorItems = emptyList()
                    )
                }
            }

            is ListsUiAction.ToggleListItemQuick -> {
                viewModelScope.launch {
                    toggleListItemUseCase(action.itemId, action.isCompleted)
                }
            }

            is ListsUiAction.ToggleListPinQuick -> {
                viewModelScope.launch {
                    toggleListPinUseCase(action.listId, action.currentPinState)
                }
            }

            is ListsUiAction.DeleteListQuick -> {
                viewModelScope.launch {
                    deleteListUseCase(action.listId)
                    sendEffect(ListsUiEffect.ShowSnackbar("List deleted"))
                }
            }

            is ListsUiAction.ClearUserMessage -> {
                updateState { it.copy(userMessage = null) }
            }
        }
    }

    private fun saveActiveEditor(pendingItemText: String = "", showSnackbar: Boolean = false) {
        val currentList = uiState.value.activeEditorList ?: return
        val currentItems = uiState.value.activeEditorItems.toMutableList()
        val trimmedPending = pendingItemText.trim()
        if (trimmedPending.isNotEmpty()) {
            currentItems.add(
                KeepListItem(
                    id = UUID.randomUUID().toString(),
                    listId = currentList.id,
                    text = trimmedPending,
                    isCompleted = false,
                    orderIndex = currentItems.size
                )
            )
        }

        val nonBlankItems = currentItems.filter { it.text.isNotBlank() }
        if (currentList.title.isNotBlank() || nonBlankItems.isNotEmpty()) {
            viewModelScope.launch {
                saveListUseCase(currentList, nonBlankItems)
                if (showSnackbar) {
                    sendEffect(ListsUiEffect.ShowSnackbar("List saved"))
                }
            }
        }
        updateState {
            it.copy(
                isEditorOpen = false,
                activeEditorList = null,
                activeEditorItems = emptyList()
            )
        }
    }
}

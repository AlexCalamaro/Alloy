package com.squidink.alloy.modules.lists.domain.usecase

import com.squidink.alloy.modules.lists.domain.model.KeepList
import com.squidink.alloy.modules.lists.domain.model.KeepListItem
import com.squidink.alloy.modules.lists.domain.model.ListWithItems
import com.squidink.alloy.modules.lists.domain.repository.IListRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe all lists with items.
 */
class GetListsWithItemsUseCase @Inject constructor(
    private val repository: IListRepository
) {
    operator fun invoke(): Flow<List<ListWithItems>> = repository.getListsWithItems()
}

/**
 * Use case to save (create or update) a list with its items.
 */
class SaveListUseCase @Inject constructor(
    private val repository: IListRepository
) {
    suspend operator fun invoke(list: KeepList, items: List<KeepListItem>) {
        val updatedList = list.copy(updatedAt = System.currentTimeMillis())
        repository.saveListWithItems(updatedList, items)
    }
}

/**
 * Use case to delete a list by ID.
 */
class DeleteListUseCase @Inject constructor(
    private val repository: IListRepository
) {
    suspend operator fun invoke(id: String) {
        repository.deleteList(id)
    }
}

/**
 * Use case to toggle completion of a checklist item.
 */
class ToggleListItemUseCase @Inject constructor(
    private val repository: IListRepository
) {
    suspend operator fun invoke(itemId: String, isCompleted: Boolean) {
        repository.toggleListItem(itemId, isCompleted)
    }
}

/**
 * Use case to toggle the pinned status of a list.
 */
class ToggleListPinUseCase @Inject constructor(
    private val repository: IListRepository
) {
    suspend operator fun invoke(listId: String, currentPinState: Boolean) {
        repository.togglePin(listId, !currentPinState)
    }
}

package com.squidink.alloy.modules.lists.domain.repository

import com.squidink.alloy.modules.lists.domain.model.KeepList
import com.squidink.alloy.modules.lists.domain.model.KeepListItem
import com.squidink.alloy.modules.lists.domain.model.ListWithItems
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing Google Keep-style checklists and checklist items.
 */
interface IListRepository {
    /**
     * Observes all lists along with their items, ordered by pinned status and last updated timestamp.
     */
    fun getListsWithItems(): Flow<List<ListWithItems>>

    /**
     * Observes a specific list by ID with its items.
     */
    fun getListWithItemsById(id: String): Flow<ListWithItems?>

    /**
     * Inserts or updates a list and synchronizes its items.
     */
    suspend fun saveListWithItems(list: KeepList, items: List<KeepListItem>)

    /**
     * Deletes a list and cascades to all its items.
     */
    suspend fun deleteList(id: String)

    /**
     * Toggles the completion status of a checklist item.
     */
    suspend fun toggleListItem(itemId: String, isCompleted: Boolean)

    /**
     * Toggles whether a list is pinned.
     */
    suspend fun togglePin(listId: String, isPinned: Boolean)

    /**
     * Updates the pastel color hex of a list.
     */
    suspend fun updateListColor(listId: String, colorHex: Long)
}

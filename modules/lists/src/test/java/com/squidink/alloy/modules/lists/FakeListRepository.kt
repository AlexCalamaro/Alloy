package com.squidink.alloy.modules.lists

import com.squidink.alloy.modules.lists.domain.model.KeepList
import com.squidink.alloy.modules.lists.domain.model.KeepListItem
import com.squidink.alloy.modules.lists.domain.model.ListWithItems
import com.squidink.alloy.modules.lists.domain.repository.IListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeListRepository(
    initialLists: List<ListWithItems> = emptyList()
) : IListRepository {

    private val listsFlow = MutableStateFlow(initialLists)

    override fun getListsWithItems(): Flow<List<ListWithItems>> = listsFlow

    override fun getListWithItemsById(id: String): Flow<ListWithItems?> =
        listsFlow.map { list -> list.find { it.list.id == id } }

    override suspend fun saveListWithItems(list: KeepList, items: List<KeepListItem>) {
        val current = listsFlow.value.filter { it.list.id != list.id }
        listsFlow.value = current + ListWithItems(list, items)
    }

    override suspend fun deleteList(id: String) {
        listsFlow.value = listsFlow.value.filter { it.list.id != id }
    }

    override suspend fun toggleListItem(itemId: String, isCompleted: Boolean) {
        listsFlow.value = listsFlow.value.map { listWithItems ->
            val updatedItems = listWithItems.items.map { item ->
                if (item.id == itemId) item.copy(isCompleted = isCompleted) else item
            }
            listWithItems.copy(items = updatedItems)
        }
    }

    override suspend fun togglePin(listId: String, isPinned: Boolean) {
        listsFlow.value = listsFlow.value.map { listWithItems ->
            if (listWithItems.list.id == listId) {
                listWithItems.copy(list = listWithItems.list.copy(isPinned = isPinned))
            } else {
                listWithItems
            }
        }
    }

    override suspend fun updateListColor(listId: String, colorHex: Long) {
        listsFlow.value = listsFlow.value.map { listWithItems ->
            if (listWithItems.list.id == listId) {
                listWithItems.copy(list = listWithItems.list.copy(colorHex = colorHex))
            } else {
                listWithItems
            }
        }
    }
}

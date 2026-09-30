package com.squidink.alloy.modules.lists.data

import com.squidink.alloy.modules.lists.db.ListDao
import com.squidink.alloy.modules.lists.db.ListEntity
import com.squidink.alloy.modules.lists.db.ListItemEntity
import com.squidink.alloy.modules.lists.domain.model.KeepList
import com.squidink.alloy.modules.lists.domain.model.KeepListItem
import com.squidink.alloy.modules.lists.domain.model.ListWithItems
import com.squidink.alloy.modules.lists.domain.repository.IListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListRepositoryImpl @Inject constructor(
    private val listDao: ListDao
) : IListRepository {

    override fun getListsWithItems(): Flow<List<ListWithItems>> {
        return listDao.getListsWithItems().map { relations ->
            relations.map { it.toDomain() }
        }
    }

    override fun getListWithItemsById(id: String): Flow<ListWithItems?> {
        return listDao.getListWithItemsById(id).map { relation ->
            relation?.toDomain()
        }
    }

    override suspend fun saveListWithItems(list: KeepList, items: List<KeepListItem>) {
        val listEntity = ListEntity.fromDomain(list)
        val itemEntities = items.map { ListItemEntity.fromDomain(it) }
        listDao.saveListWithItems(listEntity, itemEntities)
    }

    override suspend fun deleteList(id: String) {
        listDao.deleteList(id)
    }

    override suspend fun toggleListItem(itemId: String, isCompleted: Boolean) {
        listDao.updateItemCompletion(itemId, isCompleted)
    }

    override suspend fun togglePin(listId: String, isPinned: Boolean) {
        listDao.updatePinState(listId, isPinned)
    }

    override suspend fun updateListColor(listId: String, colorHex: Long) {
        listDao.updateColor(listId, colorHex)
    }
}

package com.squidink.alloy.modules.lists.data

import com.squidink.alloy.modules.lists.db.ListDao
import com.squidink.alloy.modules.lists.db.ListEntity
import com.squidink.alloy.modules.lists.db.ListItemEntity
import com.squidink.alloy.modules.lists.db.ListWithItemsRelation
import com.squidink.alloy.modules.lists.domain.model.KeepList
import com.squidink.alloy.modules.lists.domain.model.KeepListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeListDao : ListDao {
    private val lists = MutableStateFlow<List<ListEntity>>(emptyList())
    private val items = MutableStateFlow<List<ListItemEntity>>(emptyList())

    override fun getListsWithItems(): Flow<List<ListWithItemsRelation>> {
        return lists.map { listEntities ->
            listEntities.map { list ->
                ListWithItemsRelation(
                    list = list,
                    items = items.value.filter { it.listId == list.id }
                )
            }
        }
    }

    override fun getListWithItemsById(id: String): Flow<ListWithItemsRelation?> {
        return lists.map { listEntities ->
            val list = listEntities.find { it.id == id }
            list?.let {
                ListWithItemsRelation(
                    list = it,
                    items = items.value.filter { item -> item.listId == id }
                )
            }
        }
    }

    override suspend fun upsertList(list: ListEntity) {
        lists.value = lists.value.filter { it.id != list.id } + list
    }

    override suspend fun upsertItems(items: List<ListItemEntity>) {
        val newIds = items.map { it.id }.toSet()
        this.items.value = this.items.value.filter { it.id !in newIds } + items
    }

    override suspend fun deleteItemsForList(listId: String) {
        items.value = items.value.filter { it.listId != listId }
    }

    override suspend fun deleteList(id: String) {
        lists.value = lists.value.filter { it.id != id }
        deleteItemsForList(id)
    }

    override suspend fun updateItemCompletion(itemId: String, isCompleted: Boolean) {
        items.value = items.value.map {
            if (it.id == itemId) it.copy(isCompleted = isCompleted) else it
        }
    }

    override suspend fun updatePinState(listId: String, isPinned: Boolean, updatedAt: Long) {
        lists.value = lists.value.map {
            if (it.id == listId) it.copy(isPinned = isPinned, updatedAt = updatedAt) else it
        }
    }

    override suspend fun updateColor(listId: String, colorHex: Long, updatedAt: Long) {
        lists.value = lists.value.map {
            if (it.id == listId) it.copy(colorHex = colorHex, updatedAt = updatedAt) else it
        }
    }
}

class ListRepositoryImplTest {

    private lateinit var dao: FakeListDao
    private lateinit var repository: ListRepositoryImpl

    @Before
    fun setUp() {
        dao = FakeListDao()
        repository = ListRepositoryImpl(dao)
    }

    @Test
    fun `saveListWithItems and getListsWithItems correctly map between domain and entity`() = runTest {
        val list = KeepList(id = "list-1", title = "Test List", colorHex = 0xFFF28B82L, isPinned = true)
        val items = listOf(
            KeepListItem(id = "item-1", listId = "list-1", text = "Milk", isCompleted = false, orderIndex = 0),
            KeepListItem(id = "item-2", listId = "list-1", text = "Bread", isCompleted = true, orderIndex = 1)
        )

        repository.saveListWithItems(list, items)

        val result = repository.getListsWithItems().first()
        assertEquals(1, result.size)
        assertEquals("Test List", result.first().list.title)
        assertEquals(0xFFF28B82L, result.first().list.colorHex)
        assertTrue(result.first().list.isPinned)
        assertEquals(2, result.first().items.size)
        assertEquals("Milk", result.first().items[0].text)
        assertEquals("Bread", result.first().items[1].text)
        assertTrue(result.first().items[1].isCompleted)
    }

    @Test
    fun `deleteList cascades to items in dao`() = runTest {
        val list = KeepList(id = "list-1", title = "To Delete")
        val items = listOf(KeepListItem(id = "item-1", listId = "list-1", text = "Item"))
        repository.saveListWithItems(list, items)

        assertEquals(1, repository.getListsWithItems().first().size)

        repository.deleteList("list-1")
        assertTrue(repository.getListsWithItems().first().isEmpty())
    }

    @Test
    fun `toggleListItem updates completion status`() = runTest {
        val list = KeepList(id = "list-1", title = "Tasks")
        val items = listOf(KeepListItem(id = "item-1", listId = "list-1", text = "Task", isCompleted = false))
        repository.saveListWithItems(list, items)

        repository.toggleListItem("item-1", true)
        assertTrue(repository.getListsWithItems().first().first().items.first().isCompleted)
    }
}

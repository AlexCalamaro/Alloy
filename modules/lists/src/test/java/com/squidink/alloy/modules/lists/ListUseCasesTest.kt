package com.squidink.alloy.modules.lists

import com.squidink.alloy.modules.lists.domain.model.KeepList
import com.squidink.alloy.modules.lists.domain.model.KeepListItem
import com.squidink.alloy.modules.lists.domain.usecase.DeleteListUseCase
import com.squidink.alloy.modules.lists.domain.usecase.GetListsWithItemsUseCase
import com.squidink.alloy.modules.lists.domain.usecase.SaveListUseCase
import com.squidink.alloy.modules.lists.domain.usecase.ToggleListItemUseCase
import com.squidink.alloy.modules.lists.domain.usecase.ToggleListPinUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ListUseCasesTest {

    private lateinit var fakeRepo: FakeListRepository

    @Before
    fun setUp() {
        fakeRepo = FakeListRepository()
    }

    @Test
    fun `SaveListUseCase saves list and items with updated timestamp`() = runTest {
        val saveUseCase = SaveListUseCase(fakeRepo)
        val list = KeepList(id = "l1", title = "Shopping", createdAt = 1000L, updatedAt = 1000L)
        val items = listOf(KeepListItem(id = "i1", listId = "l1", text = "Apples"))

        saveUseCase(list, items)

        val retrieved = fakeRepo.getListsWithItems().first()
        assertEquals(1, retrieved.size)
        assertEquals("Shopping", retrieved.first().list.title)
        assertTrue(retrieved.first().list.updatedAt >= 1000L)
        assertEquals(1, retrieved.first().items.size)
        assertEquals("Apples", retrieved.first().items.first().text)
    }

    @Test
    fun `DeleteListUseCase deletes list by id`() = runTest {
        val saveUseCase = SaveListUseCase(fakeRepo)
        val deleteUseCase = DeleteListUseCase(fakeRepo)

        saveUseCase(KeepList(id = "l1", title = "Shopping"), emptyList())
        assertEquals(1, fakeRepo.getListsWithItems().first().size)

        deleteUseCase("l1")
        assertTrue(fakeRepo.getListsWithItems().first().isEmpty())
    }

    @Test
    fun `ToggleListItemUseCase updates item completion state`() = runTest {
        val saveUseCase = SaveListUseCase(fakeRepo)
        val toggleItemUseCase = ToggleListItemUseCase(fakeRepo)

        val list = KeepList(id = "l1", title = "Tasks")
        val item = KeepListItem(id = "i1", listId = "l1", text = "Task 1", isCompleted = false)
        saveUseCase(list, listOf(item))

        toggleItemUseCase("i1", true)
        assertTrue(fakeRepo.getListsWithItems().first().first().items.first().isCompleted)

        toggleItemUseCase("i1", false)
        assertFalse(fakeRepo.getListsWithItems().first().first().items.first().isCompleted)
    }

    @Test
    fun `ToggleListPinUseCase inverts pin state`() = runTest {
        val saveUseCase = SaveListUseCase(fakeRepo)
        val togglePinUseCase = ToggleListPinUseCase(fakeRepo)

        val list = KeepList(id = "l1", title = "Pinned Note", isPinned = false)
        saveUseCase(list, emptyList())

        togglePinUseCase("l1", currentPinState = false)
        assertTrue(fakeRepo.getListsWithItems().first().first().list.isPinned)

        togglePinUseCase("l1", currentPinState = true)
        assertFalse(fakeRepo.getListsWithItems().first().first().list.isPinned)
    }
}

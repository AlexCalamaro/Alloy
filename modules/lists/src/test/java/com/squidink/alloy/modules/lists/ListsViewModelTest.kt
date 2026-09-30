package com.squidink.alloy.modules.lists

import com.squidink.alloy.modules.lists.domain.model.KeepList
import com.squidink.alloy.modules.lists.domain.model.KeepListItem
import com.squidink.alloy.modules.lists.domain.model.ListColor
import com.squidink.alloy.modules.lists.domain.model.ListWithItems
import com.squidink.alloy.modules.lists.domain.usecase.DeleteListUseCase
import com.squidink.alloy.modules.lists.domain.usecase.GetListsWithItemsUseCase
import com.squidink.alloy.modules.lists.domain.usecase.SaveListUseCase
import com.squidink.alloy.modules.lists.domain.usecase.ToggleListItemUseCase
import com.squidink.alloy.modules.lists.domain.usecase.ToggleListPinUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ListsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepo: FakeListRepository
    private lateinit var viewModel: ListsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeListRepository()
        viewModel = ListsViewModel(
            getListsWithItemsUseCase = GetListsWithItemsUseCase(fakeRepo),
            saveListUseCase = SaveListUseCase(fakeRepo),
            deleteListUseCase = DeleteListUseCase(fakeRepo),
            toggleListItemUseCase = ToggleListItemUseCase(fakeRepo),
            toggleListPinUseCase = ToggleListPinUseCase(fakeRepo)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state observes repository lists correctly separating pinned and other`() = runTest {
        val pinned = ListWithItems(
            list = KeepList(id = "1", title = "Pinned Note", isPinned = true),
            items = listOf(KeepListItem(id = "i1", listId = "1", text = "Buy milk"))
        )
        val other = ListWithItems(
            list = KeepList(id = "2", title = "Other Note", isPinned = false),
            items = listOf(KeepListItem(id = "i2", listId = "2", text = "Read book"))
        )
        fakeRepo = FakeListRepository(listOf(pinned, other))
        viewModel = ListsViewModel(
            GetListsWithItemsUseCase(fakeRepo),
            SaveListUseCase(fakeRepo),
            DeleteListUseCase(fakeRepo),
            ToggleListItemUseCase(fakeRepo),
            ToggleListPinUseCase(fakeRepo)
        )

        val state = viewModel.uiState.value
        assertEquals(2, state.lists.size)
        assertEquals(1, state.pinnedLists.size)
        assertEquals("Pinned Note", state.pinnedLists.first().list.title)
        assertEquals(1, state.otherLists.size)
        assertEquals("Other Note", state.otherLists.first().list.title)
    }

    @Test
    fun `open create list dialog initializes empty active editor state`() {
        assertFalse(viewModel.uiState.value.isEditorOpen)

        viewModel.onAction(ListsUiAction.OpenCreateListDialog)

        val state = viewModel.uiState.value
        assertTrue(state.isEditorOpen)
        assertNotNull(state.activeEditorList)
        assertEquals("", state.activeEditorList?.title)
        assertTrue(state.activeEditorItems.isEmpty())
    }

    @Test
    fun `open edit list dialog populates target list and items`() = runTest {
        val existing = ListWithItems(
            list = KeepList(id = "10", title = "Grocery", colorHex = ListColor.CORAL.colorHex),
            items = listOf(KeepListItem(id = "item1", listId = "10", text = "Eggs"))
        )
        fakeRepo.saveListWithItems(existing.list, existing.items)

        viewModel.onAction(ListsUiAction.OpenEditListDialog("10"))

        val state = viewModel.uiState.value
        assertTrue(state.isEditorOpen)
        assertEquals("Grocery", state.activeEditorList?.title)
        assertEquals(ListColor.CORAL.colorHex, state.activeEditorList?.colorHex)
        assertEquals(1, state.activeEditorItems.size)
        assertEquals("Eggs", state.activeEditorItems.first().text)
    }

    @Test
    fun `editor actions modify editor state correctly`() {
        viewModel.onAction(ListsUiAction.OpenCreateListDialog)
        viewModel.onAction(ListsUiAction.UpdateEditorTitle("My Checklist"))
        viewModel.onAction(ListsUiAction.AddEditorItem("First item"))
        viewModel.onAction(ListsUiAction.AddEditorItem("Second item"))

        var state = viewModel.uiState.value
        assertEquals("My Checklist", state.activeEditorList?.title)
        assertEquals(2, state.activeEditorItems.size)

        val firstItemId = state.activeEditorItems[0].id
        viewModel.onAction(ListsUiAction.UpdateEditorItemText(firstItemId, "First item updated"))
        viewModel.onAction(ListsUiAction.ToggleEditorItemCompletion(firstItemId))

        state = viewModel.uiState.value
        assertEquals("First item updated", state.activeEditorItems[0].text)
        assertTrue(state.activeEditorItems[0].isCompleted)

        val secondItemId = state.activeEditorItems[1].id
        viewModel.onAction(ListsUiAction.DeleteEditorItem(secondItemId))

        state = viewModel.uiState.value
        assertEquals(1, state.activeEditorItems.size)
        assertEquals(firstItemId, state.activeEditorItems[0].id)
    }

    @Test
    fun `set editor color and pin updates editor list`() {
        viewModel.onAction(ListsUiAction.OpenCreateListDialog)
        viewModel.onAction(ListsUiAction.SetEditorColor(ListColor.MINT.colorHex))
        viewModel.onAction(ListsUiAction.ToggleEditorPin)

        val state = viewModel.uiState.value
        assertEquals(ListColor.MINT.colorHex, state.activeEditorList?.colorHex)
        assertTrue(state.activeEditorList?.isPinned == true)
    }

    @Test
    fun `save active list persists list and emits snackbar effect`() = runTest {
        val effects = mutableListOf<ListsUiEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onAction(ListsUiAction.OpenCreateListDialog)
        viewModel.onAction(ListsUiAction.UpdateEditorTitle("Persisted List"))
        viewModel.onAction(ListsUiAction.AddEditorItem("Item A"))
        viewModel.onAction(ListsUiAction.SaveActiveList())

        testScheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isEditorOpen)
        assertNull(viewModel.uiState.value.activeEditorList)
        assertEquals(1, viewModel.uiState.value.lists.size)
        assertEquals("Persisted List", viewModel.uiState.value.lists.first().list.title)
        assertEquals(1, effects.size)
        assertTrue((effects.first() as ListsUiEffect.ShowSnackbar).message.contains("saved"))

        job.cancel()
    }

    @Test
    fun `dismiss active list auto-saves when title or items exist`() = runTest {
        viewModel.onAction(ListsUiAction.OpenCreateListDialog)
        viewModel.onAction(ListsUiAction.UpdateEditorTitle("Auto Saved List"))
        viewModel.onAction(ListsUiAction.AddEditorItem("Auto Saved Item"))
        viewModel.onAction(ListsUiAction.DismissEditor())

        testScheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isEditorOpen)
        assertNull(viewModel.uiState.value.activeEditorList)
        assertEquals(1, viewModel.uiState.value.lists.size)
        assertEquals("Auto Saved List", viewModel.uiState.value.lists.first().list.title)
        assertEquals("Auto Saved Item", viewModel.uiState.value.lists.first().items.first().text)
    }

    @Test
    fun `dismiss active list saves pending item text`() = runTest {
        viewModel.onAction(ListsUiAction.OpenCreateListDialog)
        viewModel.onAction(ListsUiAction.UpdateEditorTitle("List With Pending"))
        viewModel.onAction(ListsUiAction.DismissEditor(pendingItemText = "Pending item"))

        testScheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isEditorOpen)
        assertEquals(1, viewModel.uiState.value.lists.size)
        assertEquals("List With Pending", viewModel.uiState.value.lists.first().list.title)
        assertEquals(1, viewModel.uiState.value.lists.first().items.size)
        assertEquals("Pending item", viewModel.uiState.value.lists.first().items.first().text)
    }

    @Test
    fun `dismiss active list does not save completely empty list`() = runTest {
        viewModel.onAction(ListsUiAction.OpenCreateListDialog)
        viewModel.onAction(ListsUiAction.DismissEditor())

        testScheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isEditorOpen)
        assertTrue(viewModel.uiState.value.lists.isEmpty())
    }

    @Test
    fun `insert editor item after inserts item and reindexes list`() {
        viewModel.onAction(ListsUiAction.OpenCreateListDialog)
        viewModel.onAction(ListsUiAction.AddEditorItem("Item 1"))
        viewModel.onAction(ListsUiAction.AddEditorItem("Item 2"))

        val firstItemId = viewModel.uiState.value.activeEditorItems[0].id
        viewModel.onAction(ListsUiAction.InsertEditorItemAfter(currentItemId = firstItemId, newId = "inserted-id"))

        val items = viewModel.uiState.value.activeEditorItems
        assertEquals(3, items.size)
        assertEquals(firstItemId, items[0].id)
        assertEquals(0, items[0].orderIndex)
        assertEquals("inserted-id", items[1].id)
        assertEquals("", items[1].text)
        assertEquals(1, items[1].orderIndex)
        assertEquals("Item 2", items[2].text)
        assertEquals(2, items[2].orderIndex)
    }

    @Test
    fun `quick actions toggle item completion and pin directly`() = runTest {
        val existing = ListWithItems(
            list = KeepList(id = "1", title = "Tasks", isPinned = false),
            items = listOf(KeepListItem(id = "t1", listId = "1", text = "Laundry", isCompleted = false))
        )
        fakeRepo.saveListWithItems(existing.list, existing.items)

        viewModel.onAction(ListsUiAction.ToggleListItemQuick("t1", true))
        testScheduler.runCurrent()

        assertTrue(viewModel.uiState.value.lists.first().items.first().isCompleted)

        viewModel.onAction(ListsUiAction.ToggleListPinQuick("1", false))
        testScheduler.runCurrent()

        assertTrue(viewModel.uiState.value.lists.first().list.isPinned)
    }

    @Test
    fun `delete list removes list from repository`() = runTest {
        val existing = ListWithItems(
            list = KeepList(id = "1", title = "To delete"),
            items = emptyList()
        )
        fakeRepo.saveListWithItems(existing.list, existing.items)
        assertEquals(1, viewModel.uiState.value.lists.size)

        viewModel.onAction(ListsUiAction.DeleteListQuick("1"))
        testScheduler.runCurrent()

        assertTrue(viewModel.uiState.value.lists.isEmpty())
    }
}

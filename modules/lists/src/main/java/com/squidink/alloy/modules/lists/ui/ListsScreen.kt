package com.squidink.alloy.modules.lists.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.squidink.alloy.modules.lists.ListsUiAction
import com.squidink.alloy.modules.lists.ListsUiEffect
import com.squidink.alloy.modules.lists.ListsViewModel
import com.squidink.alloy.modules.lists.R

@Composable
fun ListsScreen(
    viewModel: ListsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ListsUiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAction(ListsUiAction.OpenCreateListDialog) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.lists_new_list)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.lists.isEmpty() && !uiState.isLoading) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.lists_empty_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.lists_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 260.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    val pinned = uiState.pinnedLists
                    val others = uiState.otherLists

                    // PINNED Section
                    if (pinned.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = stringResource(R.string.lists_section_pinned),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                            )
                        }

                        items(pinned, key = { it.list.id }) { listWithItems ->
                            ListCard(
                                listWithItems = listWithItems,
                                onCardClick = { viewModel.onAction(ListsUiAction.OpenEditListDialog(listWithItems.list.id)) },
                                onToggleItem = { itemId, isCompleted ->
                                    viewModel.onAction(ListsUiAction.ToggleListItemQuick(itemId, isCompleted))
                                },
                                onTogglePin = {
                                    viewModel.onAction(ListsUiAction.ToggleListPinQuick(listWithItems.list.id, listWithItems.list.isPinned))
                                },
                                onDeleteList = {
                                    viewModel.onAction(ListsUiAction.DeleteListQuick(listWithItems.list.id))
                                }
                            )
                        }
                    }

                    // OTHERS Section Header (only show header if pinned exists too)
                    if (pinned.isNotEmpty() && others.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = stringResource(R.string.lists_section_others),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp)
                            )
                        }
                    }

                    // OTHERS / All non-pinned items
                    items(others, key = { it.list.id }) { listWithItems ->
                        ListCard(
                            listWithItems = listWithItems,
                            onCardClick = { viewModel.onAction(ListsUiAction.OpenEditListDialog(listWithItems.list.id)) },
                            onToggleItem = { itemId, isCompleted ->
                                viewModel.onAction(ListsUiAction.ToggleListItemQuick(itemId, isCompleted))
                            },
                            onTogglePin = {
                                viewModel.onAction(ListsUiAction.ToggleListPinQuick(listWithItems.list.id, listWithItems.list.isPinned))
                            },
                            onDeleteList = {
                                viewModel.onAction(ListsUiAction.DeleteListQuick(listWithItems.list.id))
                            }
                        )
                    }
                }
            }
        }
    }

    // Editor Dialog
    if (uiState.isEditorOpen && uiState.activeEditorList != null) {
        ListEditorDialog(
            list = uiState.activeEditorList!!,
            items = uiState.activeEditorItems,
            onDismiss = { pendingText -> viewModel.onAction(ListsUiAction.DismissEditor(pendingText)) },
            onTitleChange = { viewModel.onAction(ListsUiAction.UpdateEditorTitle(it)) },
            onAddItem = { viewModel.onAction(ListsUiAction.AddEditorItem(it)) },
            onInsertItemAfter = { currentId, newId -> viewModel.onAction(ListsUiAction.InsertEditorItemAfter(currentId, newId)) },
            onUpdateItemText = { itemId, text -> viewModel.onAction(ListsUiAction.UpdateEditorItemText(itemId, text)) },
            onToggleItemCompletion = { viewModel.onAction(ListsUiAction.ToggleEditorItemCompletion(it)) },
            onDeleteItem = { viewModel.onAction(ListsUiAction.DeleteEditorItem(it)) },
            onSetColor = { viewModel.onAction(ListsUiAction.SetEditorColor(it)) },
            onTogglePin = { viewModel.onAction(ListsUiAction.ToggleEditorPin) },
            onSave = { pendingText -> viewModel.onAction(ListsUiAction.SaveActiveList(pendingText)) },
            onDeleteList = { viewModel.onAction(ListsUiAction.DeleteActiveList) }
        )
    }
}

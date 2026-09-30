package com.squidink.alloy.modules.lists.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.squidink.alloy.modules.lists.domain.model.KeepList
import com.squidink.alloy.modules.lists.domain.model.KeepListItem
import com.squidink.alloy.modules.lists.domain.model.ListColor
import kotlinx.coroutines.delay
import java.util.UUID

@Composable
fun ListEditorDialog(
    list: KeepList,
    items: List<KeepListItem>,
    onDismiss: (pendingText: String) -> Unit,
    onTitleChange: (String) -> Unit,
    onAddItem: (String) -> Unit,
    onInsertItemAfter: (currentItemId: String, newId: String) -> Unit,
    onUpdateItemText: (itemId: String, text: String) -> Unit,
    onToggleItemCompletion: (itemId: String) -> Unit,
    onDeleteItem: (itemId: String) -> Unit,
    onSetColor: (Long) -> Unit,
    onTogglePin: () -> Unit,
    onSave: (pendingText: String) -> Unit,
    onDeleteList: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val listColor = ListColor.fromHex(list.colorHex)

    val dialogBg = if (listColor == ListColor.DEFAULT) {
        MaterialTheme.colorScheme.surface
    } else {
        val hex = if (isDark) listColor.darkColorHex else listColor.colorHex
        Color(hex)
    }

    val contentColor = if (listColor == ListColor.DEFAULT) {
        MaterialTheme.colorScheme.onSurface
    } else if (isDark) {
        Color.White.copy(alpha = 0.95f)
    } else {
        Color(0xFF202124)
    }

    var newItemText by remember { mutableStateOf("") }
    val titleFocusRequester = remember { FocusRequester() }
    val newItemFocusRequester = remember { FocusRequester() }
    val itemFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var pendingFocusItemId by remember { mutableStateOf<String?>(null) }

    val activeItems = items.filter { !it.isCompleted }.sortedBy { it.orderIndex }
    val completedItems = items.filter { it.isCompleted }.sortedBy { it.orderIndex }

    LaunchedEffect(Unit) {
        delay(50)
        runCatching {
            if (list.title.isEmpty() && items.isEmpty()) {
                titleFocusRequester.requestFocus()
            }
        }
    }

    LaunchedEffect(activeItems, pendingFocusItemId) {
        pendingFocusItemId?.let { idToFocus ->
            itemFocusRequesters[idToFocus]?.requestFocus()
            pendingFocusItemId = null
        }
    }

    Dialog(
        onDismissRequest = { onDismiss(newItemText) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .onKeyEvent { event ->
                    if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
                        true
                    } else {
                        false
                    }
                },
            shape = RoundedCornerShape(16.dp),
            color = dialogBg,
            contentColor = contentColor,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { onDismiss(newItemText) },
                        modifier = Modifier.focusProperties { canFocus = false }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = contentColor)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onTogglePin) {
                            Icon(
                                imageVector = if (list.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (list.isPinned) "Unpin" else "Pin",
                                tint = if (list.isPinned) MaterialTheme.colorScheme.primary else contentColor
                            )
                        }

                        IconButton(onClick = onDeleteList) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete list",
                                tint = contentColor
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(onClick = { onSave(newItemText) }) {
                            Text("Done")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title Input
                TextField(
                    value = list.title,
                    onValueChange = onTitleChange,
                    placeholder = {
                        Text(
                            "Title",
                            style = MaterialTheme.typography.titleLarge,
                            color = contentColor.copy(alpha = 0.5f)
                        )
                    },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            if (activeItems.isNotEmpty()) {
                                itemFocusRequesters[activeItems.first().id]?.requestFocus()
                            } else {
                                newItemFocusRequester.requestFocus()
                            }
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
                                if (event.type == KeyEventType.KeyDown) {
                                    if (activeItems.isNotEmpty()) {
                                        itemFocusRequesters[activeItems.first().id]?.requestFocus()
                                    } else {
                                        newItemFocusRequester.requestFocus()
                                    }
                                }
                                true
                            } else {
                                false
                            }
                        }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Items list + New Item Input
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Active Items
                    items(activeItems, key = { it.id }) { item ->
                        val focusRequester = remember(item.id) {
                            FocusRequester().also { itemFocusRequesters[item.id] = it }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.isCompleted,
                                onCheckedChange = { onToggleItemCompletion(item.id) },
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            TextField(
                                value = item.text,
                                onValueChange = { onUpdateItemText(item.id, it) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(
                                    onNext = {
                                        val currentIndex = activeItems.indexOf(item)
                                        if (currentIndex < activeItems.size - 1) {
                                            val nextItem = activeItems[currentIndex + 1]
                                            itemFocusRequesters[nextItem.id]?.requestFocus()
                                        } else {
                                            newItemFocusRequester.requestFocus()
                                        }
                                    }
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = contentColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .onPreviewKeyEvent { event ->
                                        if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
                                            if (event.type == KeyEventType.KeyDown) {
                                                val currentIndex = activeItems.indexOf(item)
                                                if (currentIndex < activeItems.size - 1) {
                                                    val newId = UUID.randomUUID().toString()
                                                    pendingFocusItemId = newId
                                                    onInsertItemAfter(item.id, newId)
                                                } else {
                                                    newItemFocusRequester.requestFocus()
                                                }
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    }
                            )
                            IconButton(
                                onClick = { onDeleteItem(item.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove item",
                                    tint = contentColor.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Add Item row
                    item {
                        val commitNewItem = {
                            val trimmed = newItemText.trim()
                            if (trimmed.isNotEmpty()) {
                                onAddItem(trimmed)
                                newItemText = ""
                            }
                            newItemFocusRequester.requestFocus()
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = contentColor.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = newItemText,
                                onValueChange = { newItemText = it },
                                placeholder = {
                                    Text(
                                        "List item",
                                        color = contentColor.copy(alpha = 0.5f)
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(
                                    onNext = { commitNewItem() },
                                    onDone = { commitNewItem() }
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = contentColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(newItemFocusRequester)
                                    .onPreviewKeyEvent { event ->
                                        if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
                                            if (event.type == KeyEventType.KeyDown) {
                                                commitNewItem()
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    }
                            )
                            if (newItemText.isNotBlank()) {
                                TextButton(
                                    onClick = { commitNewItem() }
                                ) {
                                    Text("Add")
                                }
                            }
                        }
                    }

                    // Completed Items Section
                    if (completedItems.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(
                                color = contentColor.copy(alpha = 0.15f),
                                thickness = 1.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${completedItems.size} Completed items",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = contentColor.copy(alpha = 0.6f),
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                            )
                        }

                        items(completedItems, key = { it.id }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isCompleted,
                                    onCheckedChange = { onToggleItemCompletion(item.id) },
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = contentColor.copy(alpha = 0.5f),
                                        textDecoration = TextDecoration.LineThrough
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onDeleteItem(item.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove item",
                                        tint = contentColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Row: Color Swatches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Color:",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ListColor.entries.toTypedArray()) { colorOption ->
                            val isSelected = listColor == colorOption
                            val swatchColor = if (colorOption == ListColor.DEFAULT) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                val hex = if (isDark) colorOption.darkColorHex else colorOption.colorHex
                                Color(hex)
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else contentColor.copy(alpha = 0.25f),
                                        shape = CircleShape
                                    )
                                    .clickable { onSetColor(colorOption.colorHex) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = colorOption.displayName,
                                        tint = if (isDark) Color.White else Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.squidink.alloy.modules.lists.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.squidink.alloy.modules.lists.domain.model.ListColor
import com.squidink.alloy.modules.lists.domain.model.ListWithItems

@Composable
fun ListCard(
    listWithItems: ListWithItems,
    onCardClick: () -> Unit,
    onToggleItem: (itemId: String, isCompleted: Boolean) -> Unit,
    onTogglePin: () -> Unit,
    onDeleteList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val list = listWithItems.list
    val isDark = isSystemInDarkTheme()
    val listColor = ListColor.fromHex(list.colorHex)

    val cardBg = if (listColor == ListColor.DEFAULT) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        val hex = if (isDark) listColor.darkColorHex else listColor.colorHex
        Color(hex)
    }

    val contentColor = if (listColor == ListColor.DEFAULT) {
        MaterialTheme.colorScheme.onSurface
    } else if (isDark) {
        Color.White.copy(alpha = 0.9f)
    } else {
        Color(0xFF202124)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg,
            contentColor = contentColor
        ),
        border = BorderStroke(
            1.dp,
            if (listColor == ListColor.DEFAULT) {
                MaterialTheme.colorScheme.outlineVariant
            } else {
                cardBg.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Title and Pin button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = list.title.ifBlank { "Untitled List" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (list.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (list.isPinned) "Unpin" else "Pin",
                        tint = if (list.isPinned) MaterialTheme.colorScheme.primary else contentColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Active items preview (up to 5 items)
            val displayItems = listWithItems.items.take(5)
            displayItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.isCompleted,
                        onCheckedChange = { isChecked ->
                            onToggleItem(item.id, isChecked)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (item.isCompleted) contentColor.copy(alpha = 0.5f) else contentColor,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            val remainingCount = listWithItems.items.size - displayItems.size
            if (remainingCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "+$remainingCount more items",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 30.dp)
                )
            }

            // Bottom row: Action controls
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDeleteList,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete list",
                        tint = contentColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

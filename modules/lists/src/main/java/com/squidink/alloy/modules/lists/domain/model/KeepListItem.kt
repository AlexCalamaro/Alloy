package com.squidink.alloy.modules.lists.domain.model

/**
 * Domain model representing an individual checklist item belonging to a [KeepList].
 */
data class KeepListItem(
    val id: String,
    val listId: String,
    val text: String,
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0
)

package com.squidink.alloy.modules.lists.domain.model

/**
 * Composite domain model representing a [KeepList] along with all of its associated [KeepListItem]s.
 */
data class ListWithItems(
    val list: KeepList,
    val items: List<KeepListItem>
) {
    val activeItems: List<KeepListItem>
        get() = items.filter { !it.isCompleted }.sortedBy { it.orderIndex }

    val completedItems: List<KeepListItem>
        get() = items.filter { it.isCompleted }.sortedBy { it.orderIndex }
}

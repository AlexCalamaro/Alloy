package com.squidink.alloy.modules.lists.db

import androidx.room.Embedded
import androidx.room.Relation
import com.squidink.alloy.modules.lists.domain.model.ListWithItems

data class ListWithItemsRelation(
    @Embedded
    val list: ListEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "listId"
    )
    val items: List<ListItemEntity>
) {
    fun toDomain(): ListWithItems = ListWithItems(
        list = list.toDomain(),
        items = items.map { it.toDomain() }
    )
}

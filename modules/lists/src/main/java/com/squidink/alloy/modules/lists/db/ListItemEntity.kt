package com.squidink.alloy.modules.lists.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squidink.alloy.modules.lists.domain.model.KeepListItem

@Entity(
    tableName = "keep_list_items",
    foreignKeys = [
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["listId"])]
)
data class ListItemEntity(
    @PrimaryKey
    val id: String,
    val listId: String,
    val text: String,
    val isCompleted: Boolean,
    val orderIndex: Int
) {
    fun toDomain(): KeepListItem = KeepListItem(
        id = id,
        listId = listId,
        text = text,
        isCompleted = isCompleted,
        orderIndex = orderIndex
    )

    companion object {
        fun fromDomain(item: KeepListItem): ListItemEntity = ListItemEntity(
            id = item.id,
            listId = item.listId,
            text = item.text,
            isCompleted = item.isCompleted,
            orderIndex = item.orderIndex
        )
    }
}

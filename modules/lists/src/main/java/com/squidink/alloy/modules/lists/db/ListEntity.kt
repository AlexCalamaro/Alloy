package com.squidink.alloy.modules.lists.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squidink.alloy.modules.lists.domain.model.KeepList

@Entity(tableName = "keep_lists")
data class ListEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val colorHex: Long,
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): KeepList = KeepList(
        id = id,
        title = title,
        colorHex = colorHex,
        isPinned = isPinned,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(list: KeepList): ListEntity = ListEntity(
            id = list.id,
            title = list.title,
            colorHex = list.colorHex,
            isPinned = list.isPinned,
            createdAt = list.createdAt,
            updatedAt = list.updatedAt
        )
    }
}

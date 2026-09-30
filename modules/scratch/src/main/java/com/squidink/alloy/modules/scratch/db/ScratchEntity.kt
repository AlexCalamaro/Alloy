package com.squidink.alloy.modules.scratch.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scratch_notes")
data class ScratchEntity(
    @PrimaryKey
    val id: String,
    val title: String = "Untitled Note",
    val content: String = "",
    val language: String = "PLAIN_TEXT",
    val isLocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

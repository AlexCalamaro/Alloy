package com.squidink.alloy.modules.clip.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clip_entries")
data class ClipEntity(
    @PrimaryKey
    val id: String,
    val textContent: String,
    val sourceApp: String = "Unknown",
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val mimeType: String = "text/plain"
)

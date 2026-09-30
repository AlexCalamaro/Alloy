package com.squidink.alloy.modules.lists.domain.model

/**
 * Domain model representing a Google Keep-style checklist container.
 */
data class KeepList(
    val id: String,
    val title: String,
    val colorHex: Long = 0x00000000L,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

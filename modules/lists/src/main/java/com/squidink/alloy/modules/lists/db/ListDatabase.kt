package com.squidink.alloy.modules.lists.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room Database for Google Keep-style checklists.
 * Encrypted via hardware-backed SQLCipher support open helper factory.
 */
@Database(
    entities = [
        ListEntity::class,
        ListItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ListDatabase : RoomDatabase() {
    abstract fun listDao(): ListDao
}

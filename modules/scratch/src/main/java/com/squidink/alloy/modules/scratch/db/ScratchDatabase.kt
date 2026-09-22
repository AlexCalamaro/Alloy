package com.squidink.alloy.modules.scratch.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ScratchEntity::class], version = 1, exportSchema = false)
abstract class ScratchDatabase : RoomDatabase() {
    abstract fun scratchDao(): ScratchDao
}

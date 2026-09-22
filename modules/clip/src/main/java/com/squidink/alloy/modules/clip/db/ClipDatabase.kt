package com.squidink.alloy.modules.clip.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ClipEntity::class], version = 1, exportSchema = false)
abstract class ClipDatabase : RoomDatabase() {
    abstract fun clipDao(): ClipDao
}

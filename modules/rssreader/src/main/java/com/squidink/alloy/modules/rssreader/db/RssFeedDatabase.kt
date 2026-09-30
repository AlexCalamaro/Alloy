package com.squidink.alloy.modules.rssreader.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.squidink.alloy.core.datastore.EncryptedRoomFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking

/**
 * Room database for RSS feed data.
 * Uses SQLCipher with hardware-backed encryption keys via EncryptedRoomFactory.
 */
@Database(
    entities = [
        RssFeedSubscriptionEntity::class,
        RssFeedItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RssFeedDatabase : RoomDatabase() {

    abstract fun rssFeedDao(): RssFeedDao

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface EncryptedRoomFactoryEntryPoint {
        fun encryptedRoomFactory(): EncryptedRoomFactory
    }

    companion object {
        @Volatile
        private var INSTANCE: RssFeedDatabase? = null

        const val DATABASE_NAME = "rss_reader_database"

        /**
         * Get singleton instance of the database.
         * Uses SQLCipher with hardware-backed key for encrypted storage.
         */
        fun getInstance(
            context: Context,
            openHelperFactory: SupportSQLiteOpenHelper.Factory? = null
        ): RssFeedDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    try {
                        System.loadLibrary("sqlcipher")
                    } catch (_: UnsatisfiedLinkError) {
                        // Ignored in unit test JVM environments without native libraries
                    }

                    val factory = openHelperFactory ?: runCatching {
                        val appContext = context.applicationContext
                        val entryPoint = EntryPointAccessors.fromApplication(
                            appContext,
                            EncryptedRoomFactoryEntryPoint::class.java
                        )
                        runBlocking {
                            entryPoint.encryptedRoomFactory().getFactoryFor(DATABASE_NAME)
                        }
                    }.getOrNull()

                    val builder = Room.databaseBuilder(
                        context.applicationContext,
                        RssFeedDatabase::class.java,
                        DATABASE_NAME
                    ).fallbackToDestructiveMigration(dropAllTables = true)

                    if (factory != null) {
                        builder.openHelperFactory(factory)
                    }

                    builder.build().also { INSTANCE = it }
                }
            }
        }

        /**
         * Clear the database instance. Useful for testing.
         */
        fun clearDatabase() {
            INSTANCE = null
        }
    }
}

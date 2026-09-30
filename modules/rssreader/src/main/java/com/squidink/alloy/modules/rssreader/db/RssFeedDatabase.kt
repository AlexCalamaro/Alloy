package com.squidink.alloy.modules.rssreader.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Room database for RSS feed data.
 * Uses SQLCipher for encrypted storage.
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

    companion object {
        @Volatile
        private var INSTANCE: RssFeedDatabase? = null

        private const val DATABASE_NAME = "rss_reader_database"

        /**
         * Get singleton instance of the database.
         * Uses SQLCipher for encrypted storage.
         */
        fun getInstance(context: Context): RssFeedDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    System.loadLibrary("sqlcipher")
                } catch (_: UnsatisfiedLinkError) {
                    // Ignored in unit test JVM environments without native libraries
                }

                val passphrase = "rss_reader_secret_key".toByteArray(Charsets.UTF_8)
                val factory = SupportOpenHelperFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RssFeedDatabase::class.java,
                    DATABASE_NAME
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
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

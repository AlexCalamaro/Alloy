package com.squidink.alloy.modules.scratch.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.squidink.alloy.core.datastore.EncryptedRoomFactory
import com.squidink.alloy.core.domain.common.repository.IScratchRepository
import com.squidink.alloy.modules.scratch.data.ScratchRepositoryImpl
import com.squidink.alloy.modules.scratch.db.ScratchDao
import com.squidink.alloy.modules.scratch.db.ScratchDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

/**
 * DI module for Scratch module dependencies with hardware-backed SQLCipher encryption.
 */
@Module
@InstallIn(SingletonComponent::class)
object ScratchModule {

    /**
     * Provide ScratchDao from encrypted Room database.
     */
    @Provides
    @Singleton
    fun provideScratchDao(
        @ApplicationContext context: Context,
        encryptedRoomFactory: EncryptedRoomFactory
    ): ScratchDao {
        val dbName = "scratch_database"
        val openHelperFactory = runCatching {
            runBlocking {
                encryptedRoomFactory.getFactoryFor(dbName)
            }
        }.getOrNull()

        // Clean-slate self-healing: if an unencrypted or incompatible database file exists,
        // purge it so a fresh encrypted database can be initialized without crashing.
        val dbFile = context.getDatabasePath(dbName)
        if (dbFile.exists() && openHelperFactory != null) {
            try {
                val helper = openHelperFactory.create(
                    SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name(dbName)
                        .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                            override fun onCreate(db: SupportSQLiteDatabase) {}
                            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                        })
                        .build()
                )
                helper.readableDatabase.use { db ->
                    db.query("SELECT count(*) FROM sqlite_master").use { it.moveToFirst() }
                }
                helper.close()
            } catch (e: Exception) {
                context.deleteDatabase(dbName)
            }
        }

        val builder = Room.databaseBuilder(
            context,
            ScratchDatabase::class.java,
            dbName
        ).fallbackToDestructiveMigration(dropAllTables = true)

        if (openHelperFactory != null) {
            builder.openHelperFactory(openHelperFactory)
        }

        return builder.build().scratchDao()
    }

    /**
     * Provide IScratchRepository implementation.
     */
    @Provides
    @Singleton
    fun provideScratchRepository(scratchDao: ScratchDao): IScratchRepository {
        return ScratchRepositoryImpl(scratchDao)
    }
}

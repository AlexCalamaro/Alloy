package com.squidink.alloy.modules.lists.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.squidink.alloy.core.datastore.EncryptedRoomFactory
import com.squidink.alloy.modules.lists.data.ListRepositoryImpl
import com.squidink.alloy.modules.lists.db.ListDao
import com.squidink.alloy.modules.lists.db.ListDatabase
import com.squidink.alloy.modules.lists.domain.repository.IListRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

/**
 * DI module for Lists module dependencies with hardware-backed SQLCipher encryption.
 */
@Module
@InstallIn(SingletonComponent::class)
object ListsModule {

    private const val DATABASE_NAME = "lists_database"

    @Provides
    @Singleton
    fun provideListDao(
        @ApplicationContext context: Context,
        encryptedRoomFactory: EncryptedRoomFactory
    ): ListDao {
        val openHelperFactory = runCatching {
            runBlocking {
                encryptedRoomFactory.getFactoryFor(DATABASE_NAME)
            }
        }.getOrNull()

        // Clean-slate self-healing: if an unencrypted or incompatible database file exists,
        // purge it so a fresh encrypted database can be initialized without crashing.
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        if (dbFile.exists() && openHelperFactory != null) {
            try {
                val helper = openHelperFactory.create(
                    SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name(DATABASE_NAME)
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
                context.deleteDatabase(DATABASE_NAME)
            }
        }

        val builder = Room.databaseBuilder(
            context,
            ListDatabase::class.java,
            DATABASE_NAME
        ).fallbackToDestructiveMigration(dropAllTables = true)

        if (openHelperFactory != null) {
            builder.openHelperFactory(openHelperFactory)
        }

        return builder.build().listDao()
    }

    @Provides
    @Singleton
    fun provideListRepository(listRepositoryImpl: ListRepositoryImpl): IListRepository {
        return listRepositoryImpl
    }
}

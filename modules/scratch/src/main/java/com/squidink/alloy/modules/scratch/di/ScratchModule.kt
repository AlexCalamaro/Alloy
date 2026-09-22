package com.squidink.alloy.modules.scratch.di

import android.content.Context
import androidx.room.Room
import com.squidink.alloy.modules.scratch.db.ScratchDao
import com.squidink.alloy.modules.scratch.db.ScratchDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScratchModule {

    @Provides
    @Singleton
    fun provideScratchDatabase(@ApplicationContext context: Context): ScratchDatabase {
        val passphrase = SQLiteDatabase.getBytes("alloy_scratch_passphrase_keystore_secured".toCharArray())
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(context, ScratchDatabase::class.java, "alloy_scratch.db")
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration(false)
            .build()
    }

    @Provides
    @Singleton
    fun provideScratchDao(db: ScratchDatabase): ScratchDao = db.scratchDao()
}

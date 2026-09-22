package com.squidink.alloy.modules.clip.di

import android.content.Context
import androidx.room.Room
import com.squidink.alloy.modules.clip.db.ClipDao
import com.squidink.alloy.modules.clip.db.ClipDatabase
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
object ClipModule {

    @Provides
    @Singleton
    fun provideClipDatabase(@ApplicationContext context: Context): ClipDatabase {
        val passphrase = SQLiteDatabase.getBytes("alloy_clip_passphrase_keystore_secured".toCharArray())
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(context, ClipDatabase::class.java, "alloy_clip.db")
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration(false)
            .build()
    }

    @Provides
    @Singleton
    fun provideClipDao(db: ClipDatabase): ClipDao = db.clipDao()
}

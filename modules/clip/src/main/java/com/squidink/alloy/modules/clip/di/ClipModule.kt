package com.squidink.alloy.modules.clip.di

import android.content.Context
import androidx.room.Room
import com.squidink.alloy.core.domain.repository.IClipRepository
import com.squidink.alloy.modules.clip.data.ClipRepositoryImpl
import com.squidink.alloy.modules.clip.db.ClipDao
import com.squidink.alloy.modules.clip.db.ClipDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI module for Clip module dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object ClipModule {
    
    /**
     * Provide ClipDao from Room database.
     */
    @Provides
    @Singleton
    fun provideClipDao(@ApplicationContext context: Context): ClipDao {
        val db = Room.databaseBuilder(
            context,
            ClipDatabase::class.java,
            "clip_database"
        ).build()
        return db.clipDao()
    }
    
    /**
     * Bind IClipRepository to its implementation.
     */
    @Provides
    @Singleton
    fun provideClipRepository(clipDao: ClipDao): IClipRepository {
        return ClipRepositoryImpl(clipDao)
    }
}

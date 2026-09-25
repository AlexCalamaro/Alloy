package com.squidink.alloy.modules.scratch.di

import android.content.Context
import androidx.room.Room
import com.squidink.alloy.core.domain.repository.IScratchRepository
import com.squidink.alloy.modules.scratch.data.ScratchRepositoryImpl
import com.squidink.alloy.modules.scratch.db.ScratchDao
import com.squidink.alloy.modules.scratch.db.ScratchDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI module for Scratch module dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object ScratchModule {
    
    /**
     * Provide ScratchDao from Room database.
     */
    @Provides
    @Singleton
    fun provideScratchDao(@ApplicationContext context: Context): ScratchDao {
        val db = Room.databaseBuilder(
            context,
            ScratchDatabase::class.java,
            "scratch_database"
        ).build()
        return db.scratchDao()
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

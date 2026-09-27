package com.squidink.alloy.modules.rssreader.di

import android.content.Context
import com.squidink.alloy.core.domain.common.repository.IRssFeedRepository
import com.squidink.alloy.modules.rssreader.data.RssRepositoryImpl
import com.squidink.alloy.modules.rssreader.db.RssFeedDao
import com.squidink.alloy.modules.rssreader.db.RssFeedDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI module for RSS Reader module dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object RssReaderModule {

    /**
     * Provide RssFeedDao from Room database.
     */
    @Provides
    @Singleton
    fun provideRssFeedDao(@ApplicationContext context: Context): RssFeedDao {
        val db = RssFeedDatabase.getInstance(context)
        return db.rssFeedDao()
    }

    /**
     * Bind IRssFeedRepository to its implementation.
     */
    @Provides
    @Singleton
    fun provideRssFeedRepository(rssFeedDao: RssFeedDao): IRssFeedRepository {
        return RssRepositoryImpl(rssFeedDao)
    }
}

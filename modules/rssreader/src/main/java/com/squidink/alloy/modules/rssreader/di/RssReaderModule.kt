package com.squidink.alloy.modules.rssreader.di

import android.content.Context
import com.squidink.alloy.core.domain.common.repository.IRssFeedRepository
import com.squidink.alloy.modules.rssreader.data.RssRepositoryImpl
import com.squidink.alloy.modules.rssreader.data.network.IRssHttpEngine
import com.squidink.alloy.modules.rssreader.data.network.RssHttpEngine
import com.squidink.alloy.modules.rssreader.db.RssFeedDao
import com.squidink.alloy.modules.rssreader.db.RssFeedDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * DI module for RSS Reader module dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object RssReaderModule {

    /**
     * Provide configured OkHttpClient for RSS feeds.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Provide RssFeedDao from Room database.
     */
    @Provides
    @Singleton
    fun provideRssFeedDao(@ApplicationContext context: Context): RssFeedDao {
        val db = RssFeedDatabase.getInstance(context)
        return db.rssFeedDao()
    }

    @Provides
    @Singleton
    fun provideRssHttpEngine(engine: RssHttpEngine): IRssHttpEngine = engine

    /**
     * Bind IRssFeedRepository to its implementation.
     */
    @Provides
    @Singleton
    fun provideRssFeedRepository(
        rssFeedDao: RssFeedDao,
        rssHttpEngine: IRssHttpEngine
    ): IRssFeedRepository {
        return RssRepositoryImpl(rssFeedDao, rssHttpEngine)
    }
}

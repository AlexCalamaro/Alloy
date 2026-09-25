package com.squidink.alloy.core.data.cache

/**
 * Cache policies that determine data retrieval and update strategies.
 *
 * These policies define how data sources interact with local cache
 * and remote sources to optimize performance and data freshness.
 */
sealed interface CachePolicy {
    /**
     * Always fetch from remote source, bypassing local cache.
     * Use when data freshness is critical and network is available.
     */
    data object NoCache : CachePolicy

    /**
     * Always use local cache, never contact remote source.
     * Use for offline-first scenarios or when remote is unavailable.
     */
    data object CacheOnly : CachePolicy

    /**
     * Use local cache first, refresh from remote in background.
     * Provides instant UI response while keeping data fresh.
     */
    data object CacheFirst : CachePolicy

    /**
     * Try remote first, fall back to cache on failure.
     * Ensures fresh data when possible, graceful degradation otherwise.
     */
    data object NetworkFirst : CachePolicy

    /**
     * Fetch from remote and update cache synchronously.
     * Use when you need guaranteed fresh data before proceeding.
     */
    data object Refresh : CachePolicy
}

/**
 * Extension function to check if a policy should use the local cache.
 */
fun CachePolicy.shouldUseCache(): Boolean = when (this) {
    CachePolicy.NoCache, CachePolicy.Refresh -> false
    CachePolicy.CacheOnly, CachePolicy.CacheFirst, CachePolicy.NetworkFirst -> true
}

/**
 * Extension function to check if a policy should contact the remote source.
 */
fun CachePolicy.shouldUseRemote(): Boolean = when (this) {
    CachePolicy.CacheOnly, CachePolicy.CacheFirst -> false
    CachePolicy.NoCache, CachePolicy.NetworkFirst, CachePolicy.Refresh -> true
}

package com.squidink.alloy.core.data.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.LinkedHashMap

/**
 * Simple in-memory LRU cache with thread-safe operations.
 *
 * Uses LinkedHashMap with access-order for LRU eviction policy.
 * All operations are synchronized for concurrent access safety.
 *
 * @param K The type of cache keys
 * @param V The type of cached values
 * @param maxSize Maximum number of entries before eviction starts
 */
class MemoryCache<K, V>(private val maxSize: Int = 100) {
    private val mutex = Mutex()
    
    private val cache = object : LinkedHashMap<K, V>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > maxSize
        }
    }

    /**
     * Current number of entries in the cache.
     * Note: This is a synchronous property for quick size checks.
     * For accurate size in concurrent scenarios, use [getSize].
     */
    val size: Int
        get() = cache.size

    /**
     * Get the current size of the cache.
     */
    val getSize: Int
        get() = cache.size

    /**
     * Check if cache contains a key.
     */
    suspend fun contains(key: K): Boolean = mutex.withLock {
        cache.containsKey(key)
    }

    /**
     * Get a value from the cache.
     * Returns null if key not found or value is null.
     *
     * @param key The cache key
     * @return The cached value or null
     */
    suspend fun get(key: K): V? = mutex.withLock {
        cache[key]
    }

    /**
     * Put a value into the cache.
     * Automatically evicts least recently used entries if over capacity.
     *
     * @param key The cache key
     * @param value The value to cache
     */
    suspend fun put(key: K, value: V) = mutex.withLock {
        cache[key] = value
    }

    /**
     * Remove a value from the cache.
     *
     * @param key The cache key to remove
     * @return The removed value, or null if not found
     */
    suspend fun remove(key: K): V? = mutex.withLock {
        cache.remove(key)
    }

    /**
     * Clear all entries from the cache.
     */
    suspend fun clear() = mutex.withLock {
        cache.clear()
    }

    /**
     * Get all keys in the cache.
     */
    suspend fun keys(): Set<K> = mutex.withLock {
        cache.keys.toSet()
    }

    /**
     * Get all values in the cache.
     */
    suspend fun values(): List<V> = mutex.withLock {
        cache.values.toList()
    }

    /**
     * Put multiple entries into the cache.
     *
     * @param entries Map of key-value pairs to cache
     */
    suspend fun putAll(entries: Map<K, V>) = mutex.withLock {
        cache.putAll(entries)
    }

    /**
     * Evict entries that haven't been accessed recently.
     * Called automatically when cache exceeds capacity.
     */
    suspend fun trimToSize(size: Int) {
        mutex.withLock {
            while (cache.size > size) {
                val eldest = cache.entries.first()
                cache.remove(eldest.key)
            }
        }
    }
}

/**
 * Extension function to get a value or compute it if not present.
 *
 * @param key The cache key
 * @param computeValue Function to compute the value if not cached
 * @return The cached or computed value
 */
suspend inline fun <K, V> MemoryCache<K, V>.getOrPut(
    key: K,
    crossinline computeValue: () -> V
): V {
    val existing = get(key)
    if (existing != null) {
        return existing
    }
    val newValue = computeValue()
    put(key, newValue)
    return newValue
}

package com.squidink.alloy.core.data.datasource

import kotlinx.coroutines.flow.Flow

/**
 * Interface for local data sources (e.g., Room, DataStore, in-memory cache).
 *
 * Provides CRUD operations with observable flows for reactive UI updates.
 *
 * @param T The type of data being managed
 * @param ID The type of the unique identifier
 */
interface LocalDataSource<T, ID> {
    /**
     * Retrieve a single item by its unique identifier.
     *
     * @param id The unique identifier
     * @return The item if found, null otherwise
     */
    suspend fun getById(id: ID): T?

    /**
     * Retrieve all items from the local data source.
     *
     * @return List of all items
     */
    suspend fun getAll(): List<T>

    /**
     * Insert a new item into the local data source.
     *
     * @param item The item to insert
     * @return The generated unique identifier for the inserted item
     */
    suspend fun insert(item: T): ID

    /**
     * Update an existing item in the local data source.
     *
     * @param item The item to update
     */
    suspend fun update(item: T)

    /**
     * Delete an item by its unique identifier.
     *
     * @param id The unique identifier of the item to delete
     */
    suspend fun delete(id: ID)

    /**
     * Observe a single item by its unique identifier as a Flow.
     *
     * @param id The unique identifier
     * @return Flow emitting the item when it changes
     */
    fun observeById(id: ID): Flow<T?>

    /**
     * Observe all items as a Flow.
     *
     * @return Flow emitting the list of items when they change
     */
    fun observeAll(): Flow<List<T>>
}
